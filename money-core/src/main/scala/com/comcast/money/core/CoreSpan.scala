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

import java.time.Instant
import java.util.concurrent.TimeUnit
import com.comcast.money.api._

import scala.collection.JavaConverters._
import scala.collection.concurrent.TrieMap
import io.opentelemetry.api.trace.{ SpanContext, SpanKind, StatusCode, Span => OtelSpan }
import io.opentelemetry.api.common.{ AttributeKey, Attributes }
import io.opentelemetry.context.Scope
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes

import java.lang
import scala.collection.mutable.ListBuffer

/**
 * A mutable implementation of the Span that also includes SpanInfo
 *
 * @param id The [[SpanId]]
 * @param name The name of the span
 * @param handler The [[SpanHandler]] responsible for processing the span once it is stopped
 */
private[core] case class CoreSpan(
  id: SpanId,
  var name: String,
  kind: SpanKind = SpanKind.INTERNAL,
  links: List[SpanInfo.Link] = Nil,
  startTimeNanos: Long = SystemClock.now,
  library: InstrumentationLibrary = Money.InstrumentationLibrary,
  clock: Clock = SystemClock,
  handler: SpanHandler = DisabledSpanHandler) extends Span {

  private var endTimeNanos: Long = 0
  private var status: StatusCode = StatusCode.UNSET
  private var description: String = _

  // use concurrent maps
  private val timers = new TrieMap[String, Long]()
  private val noted = new TrieMap[String, Note[_]]()
  private val events = new ListBuffer[SpanInfo.Event]()
  private var scopes: List[Scope] = Nil

  override def stop(): Unit = stop(clock.now, StatusCode.UNSET)

  override def stop(result: java.lang.Boolean): Unit =
    if (result == null) {
      stop(clock.now, StatusCode.UNSET)
    } else {
      stop(clock.now, if (result) StatusCode.OK else StatusCode.ERROR)
    }

  private def stop(endTimeNanos: Long, status: StatusCode): Unit = {
    this.endTimeNanos = endTimeNanos

    // process any hanging timers
    val openTimers = timers.keys
    openTimers.foreach(stopTimer)

    scopes.foreach { _.close() }
    scopes = Nil

    this.status = (this.status, status) match {
      case (StatusCode.UNSET, StatusCode.UNSET) => StatusCode.OK
      case (StatusCode.UNSET, other) => other
      case (other, StatusCode.UNSET) => other
      case (_, other) => other
    }

    handler.handle(info())
  }

  override def stopTimer(timerKey: String): Unit =
    timers.remove(timerKey) foreach {
      timerStartInstant =>
        record(Note.of(timerKey, System.nanoTime - timerStartInstant))
    }

  override def record(note: Note[_]): Span = {
    noted += note.name -> note
    this
  }

  override def startTimer(timerKey: String): Scope = {
    timers += timerKey -> System.nanoTime
    () => stopTimer(timerKey)
  }

  override def attachScope(scope: Scope): Span = {
    scopes = scope :: scopes
    this
  }

  override def info(): SpanInfo =
    CoreSpanInfo(
      id = id,
      name = name,
      kind = kind,
      library = library,
      startTimeNanos = startTimeNanos,
      endTimeNanos = endTimeNanos,
      durationNanos = calculateDurationNanos,
      status = status,
      description = description,
      notes = noted.toMap[String, Note[_]].asJava,
      events = events.asJava,
      links = links.asJava)

  override def close(): Unit = stop()

  override def setAttribute(attributeName: String, value: String): Span = record(Note.of(attributeName, value))
  override def setAttribute(attributeName: String, value: scala.Long): Span = record(Note.of(attributeName, value))
  override def setAttribute(attributeName: String, value: Double): Span = record(Note.of(attributeName, value))
  override def setAttribute(attributeName: String, value: Boolean): Span = record(Note.of(attributeName, value))
  override def setAttribute[T](key: AttributeKey[T], value: T): Span = record(Note.of(key, value))
  override def setAttribute(key: AttributeKey[lang.Long], value: Int): Span = setAttribute[lang.Long](key, value.toLong)

  override def addEvent(eventName: String): Span = addEventInternal(eventName, Attributes.empty(), clock.now)
  override def addEvent(eventName: String, timestamp: Instant): Span = addEventInternal(eventName, Attributes.empty(), timestamp)
  override def addEvent(eventName: String, timestamp: scala.Long, unit: TimeUnit): Span = addEventInternal(eventName, Attributes.empty(), unit.toNanos(timestamp))
  override def addEvent(eventName: String, eventAttributes: Attributes): Span = addEventInternal(eventName, eventAttributes, clock.now)
  override def addEvent(eventName: String, eventAttributes: Attributes, timestamp: Instant): Span = addEventInternal(eventName, eventAttributes, timestamp)
  override def addEvent(eventName: String, eventAttributes: Attributes, timestamp: scala.Long, unit: TimeUnit): Span = addEventInternal(eventName, eventAttributes, unit.toNanos(timestamp))

  private def addEventInternal(eventName: String, eventAttributes: Attributes, timestamp: Instant): Span = {
    val timestampNanos = if (timestamp != null)
      TimeUnit.SECONDS.toNanos(timestamp.getEpochSecond) + timestamp.getNano
    else clock.now
    addEventInternal(eventName, eventAttributes, timestampNanos)
  }

  private def addEventInternal(eventName: String, eventAttributes: Attributes, timestampNanos: scala.Long, exception: Throwable = null): Span = {
    events += CoreEvent(eventName, eventAttributes, timestampNanos, exception)
    this
  }

  override def recordException(exception: Throwable): Span = recordException(exception, Attributes.empty())
  override def recordException(exception: Throwable, eventAttributes: Attributes): Span =
    addEventInternal(SemanticAttributes.EXCEPTION_EVENT_NAME, eventAttributes, clock.now, exception)

  override def setStatus(canonicalCode: StatusCode): Span = setStatus(canonicalCode, null)

  override def setStatus(canonicalCode: StatusCode, description: String): Span = {
    this.status = canonicalCode
    this.description = description
    this
  }

  override def updateName(spanName: String): Span = {
    name = spanName
    this
  }

  override def end(): Unit = stop()
  override def `end`(endTimeStamp: Long, unit: TimeUnit): Unit = stop(unit.toNanos(endTimeStamp), StatusCode.UNSET)

  override def getSpanContext: SpanContext = id.toSpanContext

  override def isRecording: Boolean = startTimeNanos > 0 && endTimeNanos <= 0

  private def calculateDurationNanos: Long =
    if (endTimeNanos <= 0L && startTimeNanos <= 0L)
      0L
    else if (endTimeNanos <= 0L)
      clock.now - startTimeNanos
    else
      endTimeNanos - startTimeNanos
}
