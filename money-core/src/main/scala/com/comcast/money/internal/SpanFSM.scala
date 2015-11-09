package com.comcast.money.internal

import java.net.InetAddress
import java.util.concurrent.TimeUnit

import akka.actor._
import com.comcast.money.core._
import com.comcast.money.internal.EmitterBus.EmitData
import com.comcast.money.internal.EmitterProtocol.EmitSpan
import com.comcast.money.internal.SpanFSM.NoteWrapper
import com.comcast.money.internal.SpanSupervisorProtocol.SpanMessage
import com.comcast.money.metrics.MoneyMetrics
import com.comcast.money.util.DateTimeUtil

import scala.collection._
import scala.concurrent.duration._

object SpanFSMProtocol {

  sealed trait SpanCommand

  case class Start(spanId: SpanId, spanName: String,  timeStamp: Long = DateTimeUtil.microTime, parentSpanId: Option[SpanId] = None) extends SpanCommand

  case class AddNote(note: Note[_], propogate: Boolean = false) extends SpanCommand

  case class StartTimer(name: String, startTime: Long = DateTimeUtil.microTime) extends SpanCommand

  case class StopTimer(name: String, stopTime: Long = DateTimeUtil.microTime) extends SpanCommand

  case class PropagateNotesRequest(sendTo: ActorRef) extends SpanCommand

  case class PropagateNotesResponse(notes:Map[String, NoteWrapper]) extends SpanCommand

  case object Query extends SpanCommand

  case class Stop(result:Note[Boolean], timeStamp: Long = DateTimeUtil.microTime) extends SpanCommand

}

object SpanFSM {

  lazy val spanTimeout = FiniteDuration(Money.config.getDuration("money.span-timeout", TimeUnit.MILLISECONDS), MILLISECONDS)
  lazy val stoppedSpanTimeout = FiniteDuration(Money.config.getDuration("money.stopped-span-timeout", TimeUnit.MILLISECONDS), MILLISECONDS)

  // span support
  trait Timings {

    val timers: mutable.Map[String, Long]

    def startTimer(name: String, startTime: Long) = {
      timers += name -> startTime
    }

    def stopTimer(name: String, endTime: Long): Option[Note[Long]] = {
      timers.remove(name).map {
        startTime =>
          Note(name, endTime - startTime, startTime)
      }
    }
  }

  //states
  sealed trait State

  case object Idle extends State

  case object Started extends State

  case object Stopped extends State

  case object Error extends State

  //data
  sealed trait Data

  case object Empty extends Data

  case class SpanContext(spanId: SpanId,
                         spanName: String,
                         startTime: Long = DateTimeUtil.microTime,
                         notes: mutable.Map[String, NoteWrapper] = mutable.Map.empty,
                         timers: mutable.Map[String, Long] = mutable.Map.empty) extends Timings with EmitData with Data {

    private var spanSuccess = true
    private var spanDuration = 0L

    def success:Boolean = this.spanSuccess
    def setSuccess(result:Boolean):Unit = this.spanSuccess = result
    def duration:Long = this.spanDuration
    def setDuration(length:Long):Unit = this.spanDuration = length
  }

  case class NoteWrapper(note:Note[_], propagate: Boolean = false)

  def props(emitterSupervisor: ActorRef): Props = {
    Props(classOf[SpanFSM], emitterSupervisor, spanTimeout, stoppedSpanTimeout)
  }
}

import com.comcast.money.internal.SpanFSM._

class SpanFSM(val emitterSupervisor: ActorRef, val openSpanTimeout: FiniteDuration, val stoppedSpanTimeout: FiniteDuration)
  extends Actor with FSM[State, Data] with ActorLogging {

  def this(emitterSupervisor: ActorRef) = this(emitterSupervisor, 1 minute, 1 second)

  import com.comcast.money.internal.SpanFSMProtocol._

  startWith(Idle, Empty)

  MoneyMetrics(context.system).activateSpan()

  when(Idle) {
    case Event(Start(spanId: SpanId, spanName: String,  timeStamp: Long, parentSpanId:Option[SpanId]), Empty) =>
      log.debug("Idle -> Start")
      parentSpanId foreach { id => sender ! SpanMessage(id, PropagateNotesRequest(self)) }
      goto(Started) using SpanContext(spanId, spanName, timeStamp)
  }

  //stateTimeout here should prevent SpanFSM leaks from growing without bound
  when(Started, stateTimeout = openSpanTimeout) {
    case Event(PropagateNotesRequest(sendTo), spanContext: SpanContext) =>
      log.debug("Started -> PropagateNotesRes")
      val res = PropagateNotesResponse(spanContext.notes.filter(_._2.propagate))
      sendTo ! res
      stay using spanContext
    case Event(PropagateNotesResponse(parentNotes), spanContext: SpanContext) =>
      log.debug("Started -> PropagateNotesRes")
      spanContext.notes ++= parentNotes
      stay using spanContext
    case Event(AddNote(note, propagate), spanContext: SpanContext) =>
      log.debug("Started -> AddNote")
      spanContext.notes += (note.name -> NoteWrapper(note, propagate))
      stay using spanContext
    case Event(StartTimer(name, startTime), spanContext: SpanContext) =>
      log.debug("Started -> StartTimer")
      spanContext.startTimer(name, startTime)
      stay using spanContext
    case Event(StopTimer(name, stopTime), spanContext: SpanContext) =>
      log.debug("Started -> StopTimer")
      spanContext.stopTimer(name, stopTime).map {
        timerNote => spanContext.notes += (name -> NoteWrapper(timerNote))
      }
      stay using spanContext
    case Event(Stop(result, stopTime), spanContext: SpanContext) =>
      log.debug("Started -> Stopping")
      val duration = stopTime - spanContext.startTime
      spanContext.setDuration(duration)

      // Stop any outstanding timers
      for ((name, startTime) <- spanContext.timers) {
        spanContext.notes += (name -> NoteWrapper(Note(name, stopTime - startTime, startTime)))
      }
      spanContext.timers.clear()

      spanContext.setSuccess(result.value.getOrElse(true))

      goto(Stopped)
    case Event(StateTimeout, spanContext: SpanContext) =>
      MoneyMetrics(context.system).stopSpanTimeout(openSpanTimeout.toMillis)
      log.debug("Started -> Timeout")
      log.error("SpanFSM was started but timed out for spanId : {}, name: {}", spanContext.spanId, spanContext.spanName)
      stop()
    case Event(Query, spanContext: SpanContext) =>
      sender ! spanContext.copy()
      stay using spanContext
  }

  when(Stopped, stateTimeout = stoppedSpanTimeout) {
    case Event(PropagateNotesRequest(sendTo), spanContext: SpanContext) =>
      log.debug("Stopped -> PropagateNotesRes")
      val res = PropagateNotesResponse(spanContext.notes.filter(_._2.propagate))
      sendTo ! res
      stay using spanContext
    case Event(PropagateNotesResponse(parentNotes), spanContext: SpanContext) =>
      log.debug("Stopped -> PropagateNotesRes")
      spanContext.notes ++= parentNotes
      stay using spanContext
    case Event(Stop(result, stopTime), spanContext: SpanContext) =>
      log.debug("Stopped -> Stop")
      // Add the result, the basic premise here is that the last one in wins
      spanContext.setSuccess(result.value.getOrElse(true))

      // Need to update the span duration
      val duration = stopTime - spanContext.startTime
      spanContext.setDuration(duration)

      // staying using the context causes the stateTimeout to reset
      stay using spanContext

    case Event(AddNote(note, propagate), spanContext: SpanContext) =>
      log.debug("Stopped -> AddNote")
      spanContext.notes += (note.name -> NoteWrapper(note, propagate))
      stay using spanContext

    case Event(StateTimeout, data) =>
      log.debug("Stopped -> Timeout")
      stop()
  }

  onTermination {
    case StopEvent(FSM.Normal, Stopped, data: SpanContext) =>
      finishSpan(data)
  }

  whenUnhandled {
    case Event(x: State, data) =>
      log.info("Received unhandled event: " + x)
      stay()
    case Event(msg, _) =>
      log.warning("Received unknown event: " + msg)
      stop()
  }

  initialize()

  def finishSpan(tracingData: SpanContext) {
    log.debug("finishing Span")

    // it is possible that the stop was from a timeout instead of a stop message, calculate duration if not present
    val duration = DateTimeUtil.microTime - tracingData.startTime
    MoneyMetrics(context.system).stopSpan(duration)

    if (tracingData.duration <= 0)
      tracingData.setDuration(duration)

    emitterSupervisor ! EmitSpan(createSpan(tracingData))
  }

  def createSpan(spanData: SpanContext): Span = {
    Span(spanData.spanId, spanData.spanName, Money.applicationName, Money.hostName, spanData.startTime, spanData.success, spanData.duration, spanData.notes.map(x=>x._1->x._2.note).toMap)
  }
}

