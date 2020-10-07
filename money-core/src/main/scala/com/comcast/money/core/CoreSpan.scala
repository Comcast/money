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
import java.time.Instant
import java.util.Locale
import java.util.concurrent.TimeUnit

import com.comcast.money.api._

import scala.collection.JavaConverters._
import scala.collection.concurrent.TrieMap
import io.opentelemetry.trace.{ EndSpanOptions, SpanContext, StatusCanonicalCode, TraceFlags, TraceId, TraceState, SpanId => OtelSpanId }
import io.opentelemetry.common
import io.opentelemetry.common.{ AttributeKey, Attributes }
import io.opentelemetry.context.Scope
import io.opentelemetry.trace
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
  private var status: StatusCanonicalCode = StatusCanonicalCode.UNSET
  private var description: String = _

  // use concurrent maps
  private val timers = new TrieMap[String, Long]()
  private val noted = new TrieMap[String, Note[_]]()
  private val events = new ListBuffer[Event]()

  override def start(): Scope = {
    startTimeNanos = clock.now
    () => stop()
  }

  override def start(startTimeSeconds: Long, nanoAdjustment: Int): Scope = {
    startTimeNanos = TimeUnit.SECONDS.toNanos(startTimeSeconds) + nanoAdjustment
    () => stop()
  }

  override def stop(): Unit = stop(clock.now, StatusCanonicalCode.UNSET)
  override def stop(result: java.lang.Boolean): Unit =
    stop(clock.now, if (result) StatusCanonicalCode.OK else StatusCanonicalCode.ERROR)

  private def stop(endTimeNanos: Long, status: StatusCanonicalCode): Unit = {
    this.endTimeNanos = endTimeNanos

    // process any hanging timers
    val openTimers = timers.keys
    openTimers.foreach(stopTimer)

    this.status = (this.status, status) match {
      case (StatusCanonicalCode.UNSET, StatusCanonicalCode.UNSET) => StatusCanonicalCode.OK
      case (StatusCanonicalCode.UNSET, other) => other
      case (any, _) => any
    }
    if (description != null) {
      record(Note.of("description", description))
    }

    handler.handle(info())
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
  override def setAttribute[T](key: AttributeKey[T], value: T): Unit = record(Note.of(key, value))

  override def addEvent(eventName: String): Unit = addEventInternal(createEvent(eventName))
  override def addEvent(eventName: String, timestampNanos: scala.Long): Unit = addEventInternal(createEvent(eventName, Attributes.empty(), timestampNanos))
  override def addEvent(eventName: String, eventAttributes: Attributes): Unit = addEventInternal(createEvent(eventName, eventAttributes))
  override def addEvent(eventName: String, eventAttributes: Attributes, timestampNanos: scala.Long): Unit = addEventInternal(createEvent(eventName, eventAttributes, timestampNanos))

  private def addEventInternal(event: Event): Unit = events :+ event

  private def createEvent(eventName: String, eventAttributes: Attributes = Attributes.empty, timestampNanos: Long = clock.now): Event = new Event {
    override def timestamp: Long = timestampNanos
    override def name: String = eventName
    override def attributes: Attributes = eventAttributes
  }

  override def recordException(exception: Throwable): Unit = recordException(exception, Attributes.empty())
  override def recordException(exception: Throwable, eventAttributes: Attributes): Unit = {
    val timestampNanos = clock.now

    val attributes = if (eventAttributes != null) {
      eventAttributes.toBuilder
    } else {
      Attributes.newBuilder
    }
    attributes.setAttribute(SemanticAttributes.EXCEPTION_TYPE, exception.getClass.getCanonicalName)
    if (exception.getMessage != null) {
      attributes.setAttribute(SemanticAttributes.EXCEPTION_MESSAGE, exception.getMessage)
    }
    val writer = new StringWriter
    exception.printStackTrace(new PrintWriter(writer))
    attributes.setAttribute(SemanticAttributes.EXCEPTION_STACKTRACE, writer.toString)

    addEvent(SemanticAttributes.EXCEPTION_EVENT_NAME, attributes.build(), timestampNanos)
  }

  override def setStatus(canonicalCode: StatusCanonicalCode): Unit = this.status = canonicalCode

  override def setStatus(canonicalCode: StatusCanonicalCode, description: String): Unit = {
    this.status = canonicalCode
    this.description = description
  }

  override def updateName(spanName: String): Unit = name = spanName

  override def end(): Unit = stop()
  override def end(endSpanOptions: EndSpanOptions): Unit = stop(endSpanOptions.getEndTimestamp, StatusCanonicalCode.UNSET)

  override def getContext: SpanContext = {
    val traceId = id.traceId.replace("-", "").toLowerCase(Locale.US)
    val spanId = OtelSpanId.fromLong(id.selfId)
    SpanContext.create(traceId, spanId, TraceFlags.getDefault, TraceState.getDefault)
  }

  override def isRecording: Boolean = this.status == StatusCanonicalCode.UNSET

  private def toMillis(nanos: Long): Long = TimeUnit.NANOSECONDS.toMillis(nanos)
  private def toMicros(nanos: Long): Long = TimeUnit.NANOSECONDS.toMicros(nanos)

  private def calculateDurationMicros: Long =
    if (endTimeNanos <= 0L && startTimeNanos <= 0L)
      0L
    else if (endTimeNanos <= 0L)
      toMicros(clock.now - startTimeNanos)
    else
      toMicros(endTimeNanos - startTimeNanos)

  private def success: java.lang.Boolean = this.status match {
    case StatusCanonicalCode.OK => true
    case StatusCanonicalCode.ERROR => false
    case _ => null
  }
}
