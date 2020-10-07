/*
 * Copyright 2012 Comcast Cable Communications Management, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.comcast.money.core

import java.io.{ PrintWriter, StringWriter }
import java.util.Locale
import java.util.concurrent.TimeUnit

import com.comcast.money.api._

import scala.collection.JavaConverters._
import scala.collection.concurrent.TrieMap
import io.opentelemetry.trace.{ EndSpanOptions, Event, SpanContext, Status, TraceFlags, TraceId, TraceState }
import io.opentelemetry.common.AttributeValue
import io.opentelemetry.common.Attributes
import io.opentelemetry.context.Scope
import io.opentelemetry.trace
import io.opentelemetry.trace.Status.CanonicalCode
import io.opentelemetry.trace.attributes.SemanticAttributes

import scala.collection.mutable.ListBuffer

/**
 * A mutable implementation of the Span that also includes SpanInfo
 *
 * @param id The [[SpanId]]
 * @param name The name of the span
 * @param handler The [[SpanHandler]] responsible for processing the span once it is stopped
 */
case class CoreSpan(
  id: SpanId,
  var name: String,
  clock: Clock,
  handler: SpanHandler) extends Span {

  private var startTimeNanos: Long = 0
  private var endTimeNanos: Long = 0
  private var status: Status = Status.OK
  private var success: java.lang.Boolean = _

  // use concurrent maps
  private val timers = new TrieMap[String, Long]()
  private val noted = new TrieMap[String, Note[_]]()
  private val events = new ListBuffer[EventWithTimestamp]()

  override def start(): Scope = {
    startTimeNanos = clock.now
    () => stop()
  }

  override def stop(): Unit = stop(clock.now, this.status)
  override def stop(result: java.lang.Boolean): Unit =
    stop(clock.now, if (result) Status.OK else Status.UNKNOWN)

  private def stop(endTimeNanos: Long, status: Status): Unit = {
    this.endTimeNanos = endTimeNanos

    this.status = status
    this.success = recordStatus(status)

    // process any hanging timers
    val openTimers = timers.keys
    openTimers.foreach(stopTimer)

    handler.handle(info())
  }

  private def recordStatus(status: Status): Boolean = {
    status.getCanonicalCode match {
      case CanonicalCode.OK =>
      case CanonicalCode.UNKNOWN =>
      case code: CanonicalCode =>
        record(Note.of("span-status", code.name))
    }
    Option(status.getDescription) match {
      case Some(description) if !description.isEmpty =>
        record(Note.of("span-status-description", description))
      case _ =>
    }
    status.isOk
  }

  override def stopTimer(timerKey: String): Unit =
    timers.remove(timerKey) foreach {
      timerStartInstant =>
        record(Note.of(timerKey, System.nanoTime - timerStartInstant))
    }

  override def record(note: Note[_]): Unit = noted += note.name -> note

  override def startTimer(timerKey: String): Scope = {
    timers += timerKey -> System.nanoTime
    () => stopTimer(timerKey)
  }

  override def info(): SpanInfo =
    CoreSpanInfo(
      id = id,
      name = name,
      startTimeMillis = toMillis(startTimeNanos),
      startTimeMicros = toMicros(startTimeNanos),
      endTimeMillis = toMillis(endTimeNanos),
      endTimeMicros = toMicros(endTimeNanos),
      durationMicros = calculateDurationMicros,
      success = success,
      notes = noted.toMap[String, Note[_]].asJava,
      events = events.asJavaCollection)

  override def close(): Unit = stop()

  override def setAttribute(attributeName: String, value: String): Unit = record(Note.of(attributeName, value))
  override def setAttribute(attributeName: String, value: scala.Long): Unit = record(Note.of(attributeName, value))
  override def setAttribute(attributeName: String, value: Double): Unit = record(Note.of(attributeName, value))
  override def setAttribute(attributeName: String, value: Boolean): Unit = record(Note.of(attributeName, value))
  override def setAttribute(attributeName: String, value: AttributeValue): Unit = record(Note.fromAttributeValue(name, value))

  override def addEvent(eventName: String): Unit = addEventInternal(createEvent(eventName, Attributes.empty()))
  override def addEvent(eventName: String, timestampNanos: scala.Long): Unit = addEventInternal(createEvent(eventName, Attributes.empty(), timestampNanos))
  override def addEvent(eventName: String, eventAttributes: Attributes): Unit = addEventInternal(createEvent(eventName, eventAttributes))
  override def addEvent(eventName: String, eventAttributes: Attributes, timestampNanos: scala.Long): Unit = addEventInternal(createEvent(eventName, eventAttributes, timestampNanos))
  override def addEvent(event: Event): Unit = addEventInternal(createEvent(event, clock.now))
  override def addEvent(event: Event, timestampNanos: scala.Long): Unit = addEventInternal(createEvent(event, timestampNanos))

  private def addEventInternal(event: EventWithTimestamp): Unit = events :+ event

  private def createEvent(event: Event, timestampNanos: Long): EventWithTimestamp = event match {
    case eventWithTimestamp: EventWithTimestamp if eventWithTimestamp.timestamp == timestampNanos => eventWithTimestamp
    case _ => new EventWithTimestamp {
      override def timestamp(): Long = timestampNanos
      override def getName: String = event.getName
      override def getAttributes: Attributes = event.getAttributes
      override def getEvent: Event = event
    }
  }

  private def createEvent(eventName: String, eventAttributes: Attributes, timestampNanos: Long = clock.now): EventWithTimestamp = new EventWithTimestamp {
    override def timestamp(): Long = timestampNanos
    override def getName: String = eventName
    override def getAttributes: Attributes = eventAttributes
  }

  override def recordException(exception: Throwable): Unit = recordException(exception, Attributes.empty())
  override def recordException(exception: Throwable, eventAttributes: Attributes): Unit = {
    val timestampNanos = clock.now

    val attributes = if (eventAttributes != null) {
      eventAttributes.toBuilder
    } else {
      Attributes.newBuilder
    }
    SemanticAttributes.EXCEPTION_TYPE.set(attributes, exception.getClass.getCanonicalName)
    if (exception.getMessage != null) {
      SemanticAttributes.EXCEPTION_MESSAGE.set(attributes, exception.getMessage)
    }
    val writer = new StringWriter
    exception.printStackTrace(new PrintWriter(writer))
    SemanticAttributes.EXCEPTION_STACKTRACE.set(attributes, writer.toString)

    addEventInternal(new EventWithTimestamp {
      override def timestamp(): Long = timestampNanos
      override def getName: String = SemanticAttributes.EXCEPTION_EVENT_NAME
      override def getAttributes: Attributes = attributes.build()
    })
  }

  override def setStatus(status: Status): Unit = this.status = status

  override def updateName(spanName: String): Unit = name = spanName

  override def end(): Unit = stop()
  override def end(endSpanOptions: EndSpanOptions): Unit = stop(endSpanOptions.getEndTimestamp, this.status)

  override def getContext: SpanContext = new SpanContext {
    override def getTraceId: TraceId =
      TraceId.fromLowerBase16(id.traceId.replace("-", "").toLowerCase(Locale.US), 0)

    override def getSpanId: trace.SpanId =
      trace.SpanId.fromLowerBase16(f"${id.selfId}%016x", 0)

    override def getTraceState: TraceState = TraceState.getDefault
    override def getTraceFlags: TraceFlags = TraceFlags.getDefault
    override def isRemote: Boolean = false
  }

  override def isRecording: Boolean = this.success == null

  private def toMillis(nanos: Long): Long = TimeUnit.NANOSECONDS.toMillis(nanos)
  private def toMicros(nanos: Long): Long = TimeUnit.NANOSECONDS.toMicros(nanos)

  private def calculateDurationMicros: Long =
    if (endTimeNanos <= 0L && startTimeNanos <= 0L)
      0L
    else if (endTimeNanos <= 0L)
      toMicros(clock.now - startTimeNanos)
    else
      toMicros(endTimeNanos - startTimeNanos)
}
