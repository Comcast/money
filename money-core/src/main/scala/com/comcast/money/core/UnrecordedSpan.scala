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

import com.comcast.money.api.{ Note, Span, SpanId, SpanInfo }
import io.opentelemetry.api.common.{ AttributeKey, Attributes }
import io.opentelemetry.context.Scope
import io.opentelemetry.api.trace.{ SpanContext, StatusCode }

private[core] final case class UnrecordedSpan(
  spanId: SpanId,
  var name: String) extends Span {

  private var scopes: List[Scope] = Nil

  override def info(): SpanInfo = CoreSpanInfo(spanId, name)
  override def getSpanContext: SpanContext = spanId.toSpanContext

  override def attachScope(scope: Scope): Span = {
    scopes = scope :: scopes
    this
  }

  override def close(): Unit = {
    scopes.foreach { _.close() }
    scopes = Nil
  }
  override def isRecording: Boolean = false

  // $COVERAGE-OFF$
  override def `end`(): Unit = close()
  override def `end`(endTimeStamp: Long, unit: TimeUnit): Unit = close()

  override def record(note: Note[_]): Span = this
  override def startTimer(timerKey: String): Scope = () => ()
  override def stopTimer(timerKey: String): Unit = ()
  override def setAttribute(key: String, value: String): Span = this
  override def setAttribute(key: String, value: Long): Span = this
  override def setAttribute(key: String, value: Double): Span = this
  override def setAttribute(key: String, value: Boolean): Span = this
  override def setAttribute[T](key: AttributeKey[T], value: T): Span = this
  override def addEvent(name: String): Span = this
  override def addEvent(name: String, timestamp: Long, unit: TimeUnit): Span = this
  override def addEvent(name: String, timestamp: Instant): Span = this
  override def addEvent(name: String, attributes: Attributes): Span = this
  override def addEvent(name: String, attributes: Attributes, timestamp: Long, unit: TimeUnit): Span = this
  override def addEvent(name: String, attributes: Attributes, timestamp: Instant): Span = this
  override def setStatus(canonicalCode: StatusCode): Span = this
  override def setStatus(canonicalCode: StatusCode, description: String): Span = this
  override def recordException(exception: Throwable): Span = this
  override def recordException(exception: Throwable, additionalAttributes: Attributes): Span = this
  override def updateName(name: String): Span = this
  // $COVERAGE-ON$
}
