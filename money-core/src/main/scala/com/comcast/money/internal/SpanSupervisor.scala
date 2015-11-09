package com.comcast.money.internal

import akka.actor._
import com.comcast.money.core.SpanId
import com.comcast.money.akka.ActorMaker
import com.comcast.money.internal.SpanFSMProtocol.{PropagateNotesRequest, SpanCommand, Start}

object SpanSupervisorProtocol {

  case class SpanMessage(spanId: SpanId, command: SpanCommand)
}

object SpanSupervisor {

  def props(emitterSupervisorRef: ActorRef): Props = {
    Props(classOf[SpanSupervisor], emitterSupervisorRef)
  }
}

class SpanSupervisor(emitterSupervisorRef: ActorRef) extends Actor
  with ActorMaker with ActorLogging {

  import SpanSupervisorProtocol._

  def receive = {

    case SpanMessage(spanId: SpanId, message: Start)  =>
      val spanFSMRef = makeActor(SpanFSM.props(emitterSupervisorRef), spanId.toString)
      spanFSMRef ! message

    case SpanMessage(spanId: SpanId, message: SpanCommand)  =>
      context.child(spanId.toString) match {
        case Some(spanFSMRef) =>
          spanFSMRef forward message
        case None =>
          if (!message.isInstanceOf[PropagateNotesRequest])
            log.warning("Attempted to message non-existent SpanFSM for spanId {}, w/ message: {}", spanId, message)
      }
  }
}