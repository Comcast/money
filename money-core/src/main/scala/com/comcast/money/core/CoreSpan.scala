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

import com.comcast.money.api._
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.{ SpanContext, SpanKind, StatusCode }
import io.opentelemetry.context.Scope

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import scala.collection.JavaConverters._
import scala.collection.concurrent.TrieMap
import scala.collection.mutable.ListBuffer

/**
 * A mutable implementation of the Span that also includes SpanInfo
 *
 * @param id The [[SpanId]]
 * @param name The name of the span
 * @param handler The [[SpanHandler]] responsible for processing the span once it is stopped
 */
private[core] case class CoreSpan(
  override val id: SpanId,
  var name: String,
  kind: SpanKind = SpanKind.INTERNAL,
  links: List[LinkInfo] = Nil,
  startTimeNanos: Long = SystemClock.now,
  library: InstrumentationLibrary = Money.InstrumentationLibrary,
  clock: Clock = SystemClock,
  handler: SpanHandler = DisabledSpanHandler) extends Span {

  private val ended: AtomicBoolean = new AtomicBoolean()

  private var endTimeNanos: Long = 0
  private var status: StatusCode = StatusCode.UNSET
  private var description: String = _

  // use concurrent maps
  private val timers = new TrieMap[String, Long]()
  private val noted = new TrieMap[String, Note[_]]()
  private val events = new ListBuffer[EventInfo]()
  private var scopes: List[Scope] = Nil

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
    CoreSpanInfo.builder()
      .appName(Money.Environment.applicationName)
      .host(Money.Environment.hostName)
      .library(library)
      .id(id)
      .name(name)
      .kind(kind)
      .links(links.asJava)
      .startTimeNanos(startTimeNanos)
      .endTimeNanos(endTimeNanos)
      .hasEnded(ended.get())
      .durationNanos(calculateDurationNanos)
      .status(status)
      .description(description)
      .notes(noted.toMap[String, Note[_]].asJava)
      .events(events.asJava)
      .build()

  override def close(): Unit = end()

  override def addEvent(eventName: String, eventAttributes: Attributes): Span = addEvent(eventName, eventAttributes, clock.now, TimeUnit.NANOSECONDS)
  override def addEvent(eventName: String, eventAttributes: Attributes, timestamp: scala.Long, unit: TimeUnit): Span = {
    events += new CoreEvent(eventName, unit.toNanos(timestamp), eventAttributes)
    this
  }

  override def recordException(exception: Throwable, eventAttributes: Attributes): Span = {
    events += new CoreExceptionEvent(exception, clock.now, eventAttributes)
    this
  }

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

  override def end(): Unit = end(clock.now, TimeUnit.NANOSECONDS)

  override def end(timestamp: Long, unit: TimeUnit): Unit =
    if (ended.compareAndSet(false, true)) {
      this.endTimeNanos = unit.toNanos(timestamp)

      // process any hanging timers
      val openTimers = timers.keys
      openTimers.foreach(stopTimer)

      scopes.foreach {
        _.close()
      }
      scopes = Nil

      handler.handle(info())
    }

  override def getSpanContext: SpanContext = id.toSpanContext

  override def isRecording: Boolean = true

  private def calculateDurationNanos: Long =
    if (endTimeNanos <= 0L && startTimeNanos <= 0L)
      0L
    else if (endTimeNanos <= 0L)
      clock.now - startTimeNanos
    else
      endTimeNanos - startTimeNanos
}
