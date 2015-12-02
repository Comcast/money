package com.comcast.money.api

import java.lang
import java.lang.{Double, Long}

import scala.collection._
import scala.collection.JavaConversions._

// obviously not threadsafe and very mutable, basing this kinda on what I did for the java-only core
class DefaultSpan(
  val id: SpanId,
  val spanName: String,
  spanMonitor: SpanMonitor,
  notes: mutable.Map[String, Note[_]] = new mutable.HashMap[String, Note[_]],
  propagate: Boolean = false) extends Span {

  // TODO: yea, hacky, just prototyping though
  private var stopTime: Long = _
  private var stopInstant: Long = _
  private var startedTime: Long = _
  private var startInstant: Long = _
  private var success: Boolean = true
  private val timers = new mutable.HashMap[String, Long]

  override def startTime(): Long = startedTime

  override def start(): Unit = {
    startedTime = System.currentTimeMillis
    startInstant = System.nanoTime

    // Start watching me
    spanMonitor.watch(this)

    // Push me onto thread local
    SpanLocal.push(this)
  }

  override def childSpan(childName: String, propagate: Boolean): Span = {
    if (propagate)
      new DefaultSpan(
        id.newChild(),
        childName,
        spanMonitor,
        notes.filter(propagatedNotes),
        propagate)
    else
      new DefaultSpan(
        id.newChild(),
        childName,
        spanMonitor
      )
  }

  override def stop(): Unit = stop(true)

  override def stop(result: Boolean): Unit = {
    stopTime = System.currentTimeMillis
    stopInstant = System.nanoTime
    success = result

    // stop watching me
    spanMonitor.unwatch(this)

    // Remove this span from span local if it is the one in scope
    // NOTE not sure if we are cleaning up thread local properly yet
    SpanLocal.current.foreach {
      current =>
        if (current.id == this.id)
          SpanLocal.pop()
    }
  }

  override def stopTimer(timerKey: String): Unit =
    timers.remove(timerKey).foreach(record(timerKey, _))

  override def startTimer(timerKey: String): Unit =
    timers += timerKey -> System.nanoTime

  override def data(): SpanData =
    new SpanData(notes, startedTime, stopTime, success, id, spanName, stopInstant - startInstant)

  override def record(key: String, value: String): Unit =
    notes += key -> Note.of(key, value)

  override def record(key: String, value: lang.Boolean): Unit =
    notes += key -> Note.of(key, value)

  override def record(key: String, value: Double): Unit =
    notes += key -> Note.of(key, value)

  override def record(key: String, value: Long): Unit =
    notes += key -> Note.of(key, value)

  override def record(key: String, value: String, propagate: Boolean): Unit =
    notes += key -> Note.of(key, value, propagate)

  override def record(key: String, value: lang.Boolean, propagate: Boolean): Unit =
    notes += key -> Note.of(key, value, propagate)

  override def record(key: String, value: Double, propagate: Boolean): Unit =
    notes += key -> Note.of(key, value, propagate)

  override def record(key: String, value: Long, propagate: Boolean): Unit =
    notes += key -> Note.of(key, value, propagate)

  private def propagatedNotes: ((String, Note[_])) => Boolean = tuple => tuple._2.isPropagated
}
