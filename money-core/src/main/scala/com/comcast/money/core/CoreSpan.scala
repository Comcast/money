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

import java.lang.Long

import io.opentelemetry.trace.{ Span => OTelSpan }
import com.comcast.money.api._

import scala.collection.JavaConverters._
import scala.collection.concurrent.TrieMap
import io.opentelemetry.trace.EndSpanOptions
import io.opentelemetry.common.AttributeValue
import io.opentelemetry.common.Attributes
import io.opentelemetry.trace.Event
import io.opentelemetry.trace.Status
import io.opentelemetry.trace.SpanContext
import io.opentelemetry.common.AttributeValue.Type

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
  handler: SpanHandler) extends Span {

  private var startTimeMillis: Long = 0L
  private var startTimeMicros: Long = 0L
  private var endTimeMillis: Long = 0L
  private var endTimeMicros: Long = 0L
  private var success: Boolean = true

  // use concurrent maps
  private val timers = new TrieMap[String, Long]()
  private val noted = new TrieMap[String, Note[_]]()

  def start(): Unit = {
    startTimeMillis = System.currentTimeMillis
    startTimeMicros = System.nanoTime / 1000
  }

  def stop(): Unit = stop(System.currentTimeMillis())
  def stop(result: java.lang.Boolean): Unit = stop(System.currentTimeMillis(), if (result) Status.OK else Status.UNKNOWN)
  def stop(endTimeMillis: scala.Long, result: java.lang.Boolean): Unit = stop(endTimeMillis, if (result) Status.OK else Status.UNKNOWN)
  def stop(endTimeMillis: scala.Long, status: Status): Unit = {
    this.success = status.isOk
    stop(endTimeMillis)
  }

  def stop(endTimeMillis: scala.Long): Unit = {
    this.endTimeMillis = endTimeMillis
    endTimeMicros = System.nanoTime / 1000

    // process any hanging timers
    val openTimers = timers.keys
    openTimers.foreach(stopTimer)

    handler.handle(info)
  }

  def stopTimer(timerKey: String): Unit =
    timers.remove(timerKey) foreach {
      timerStartInstant =>
        record(Note.of(timerKey, System.nanoTime - timerStartInstant))
    }

  def record(note: Note[_]): Unit = noted += note.name -> note

  def startTimer(timerKey: String): Unit = timers += timerKey -> System.nanoTime

  def info(): SpanInfo =
    CoreSpanInfo(
      id,
      name,
      startTimeMillis,
      startTimeMicros,
      endTimeMillis,
      endTimeMicros,
      calculateDuration,
      success,
      noted.toMap[String, Note[_]].asJava)

  override def setAttribute(attributeName: String, value: String): Unit = record(Note.of(attributeName, value))

  override def setAttribute(attributeName: String, value: scala.Long): Unit = record(Note.of(attributeName, value))

  override def setAttribute(attributeName: String, value: Double): Unit = record(Note.of(attributeName, value))

  override def setAttribute(attributeName: String, value: Boolean): Unit = record(Note.of(attributeName, value))

  override def setAttribute(attributeName: String, value: AttributeValue): Unit = ???

  override def addEvent(eventName: String): Unit = ???

  override def addEvent(eventName: String, timestampNanos: scala.Long): Unit = ???

  override def addEvent(eventName: String, eventAttributes: Attributes): Unit = ???

  override def addEvent(eventName: String, eventAttributes: Attributes, timestampNanos: scala.Long): Unit = ???

  override def addEvent(event: Event): Unit = ???

  override def addEvent(event: Event, timestampNanos: scala.Long): Unit = ???

  override def setStatus(status: Status): Unit = ???

  override def recordException(exceptions: Throwable): Unit = ???

  override def recordException(exception: Throwable, eventAttributes: Attributes): Unit = ???

  override def updateName(spanName: String): Unit = name = spanName

  override def end(): Unit = stop()

  override def end(endSpanOptions: EndSpanOptions): Unit = stop(endSpanOptions.getEndTimestamp())

  override def getContext(): SpanContext = ???

  override def isRecording(): Boolean = ???

  private def calculateDuration: Long =
    if (endTimeMicros <= 0L && startTimeMicros <= 0L)
      0L
    else if (endTimeMicros <= 0L)
      (System.nanoTime() / 1000) - startTimeMicros
    else
      endTimeMicros - startTimeMicros
}
