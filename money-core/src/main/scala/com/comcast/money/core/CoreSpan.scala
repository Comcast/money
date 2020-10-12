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
import io.opentelemetry.trace.{ EndSpanOptions, SpanContext, Span => OtelSpan, StatusCanonicalCode, TraceFlags, TraceId, TraceState, SpanId => OtelSpanId }
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
private[core] case class CoreSpan(
  id: SpanId,
  var name: String,
  var kind: OtelSpan.Kind = OtelSpan.Kind.INTERNAL,
  clock: Clock = SystemClock,
  handler: SpanHandler) extends Span {

  private var startTimeNanos: Long = 0
  private var endTimeNanos: Long = 0
  private var status: StatusCanonicalCode = StatusCanonicalCode.UNSET
  private var description: String = _

  // use concurrent maps
  private val timers = new TrieMap[String, Long]()
  private val noted = new TrieMap[String, Note[_]]()
  private val events = new ListBuffer[Event]()
  private var scopes: List[Scope] = Nil

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
    if (result == null) {
      stop(clock.now, StatusCanonicalCode.UNSET)
    } else {
      stop(clock.now, if (result) StatusCanonicalCode.OK else StatusCanonicalCode.ERROR)
    }

  private def stop(endTimeNanos: Long, status: StatusCanonicalCode): Unit = {
    this.endTimeNanos = endTimeNanos

    // process any hanging timers
    val openTimers = timers.keys
    openTimers.foreach(stopTimer)

    scopes.foreach { _.close() }
    scopes = Nil

    this.status = (this.status, status) match {
      case (StatusCanonicalCode.UNSET, StatusCanonicalCode.UNSET) => StatusCanonicalCode.OK
      case (StatusCanonicalCode.UNSET, other) => other
      case (other, StatusCanonicalCode.UNSET) => other
      case (_, other) => other
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

  override def attachScope(scope: Scope): Span = {
    scopes = scope :: scopes
    this
  }

  override def info(): SpanInfo =
    CoreSpanInfo(
      id = id,
      name = name,
      kind = kind,
      startTimeNanos = startTimeNanos,
      endTimeNanos = endTimeNanos,
      durationNanos = calculateDurationNanos,
      status = status,
      description = description,
      notes = noted.toMap[String, Note[_]].asJava,
      events = events.asJava)

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

  private def addEventInternal(event: Event): Unit = events += event

  private def createEvent(
    eventName: String,
    eventAttributes: Attributes = Attributes.empty,
    timestampNanos: Long = clock.now,
    exception: Throwable = null): Event =
    CoreEvent(eventName, eventAttributes, timestampNanos, exception)

  override def recordException(exception: Throwable): Unit = recordException(exception, Attributes.empty())
  override def recordException(exception: Throwable, eventAttributes: Attributes): Unit =
    addEventInternal(createEvent(SemanticAttributes.EXCEPTION_EVENT_NAME, eventAttributes, clock.now, exception))

  override def setStatus(canonicalCode: StatusCanonicalCode): Unit = this.status = canonicalCode

  override def setStatus(canonicalCode: StatusCanonicalCode, description: String): Unit = {
    this.status = canonicalCode
    this.description = description
  }

  override def updateName(spanName: String): Unit = name = spanName
  override def updateKind(spanKind: OtelSpan.Kind): Unit = kind = spanKind

  override def end(): Unit = stop()
  override def end(endSpanOptions: EndSpanOptions): Unit = stop(endSpanOptions.getEndTimestamp, StatusCanonicalCode.UNSET)

  override def getContext: SpanContext = id.toSpanContext

  override def isRecording: Boolean = startTimeNanos > 0 && endTimeNanos <= 0

  private def calculateDurationNanos: Long =
    if (endTimeNanos <= 0L && startTimeNanos <= 0L)
      0L
    else if (endTimeNanos <= 0L)
      clock.now - startTimeNanos
    else
      endTimeNanos - startTimeNanos
}
