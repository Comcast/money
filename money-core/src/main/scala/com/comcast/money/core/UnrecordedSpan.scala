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

import java.lang

import com.comcast.money.api.{ Note, Span, SpanId, SpanInfo }
import io.opentelemetry.common.{ AttributeKey, Attributes }
import io.opentelemetry.context.Scope
import io.opentelemetry.trace.{ EndSpanOptions, Span => OtelSpan, SpanContext, StatusCanonicalCode }

private[core] final case class UnrecordedSpan(
  spanId: SpanId,
  var name: String) extends Span {

  private var scopes: List[Scope] = Nil

  override def info(): SpanInfo = CoreSpanInfo(spanId, name)
  override def getContext: SpanContext = spanId.toSpanContext

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
  override def stop(): Unit = close()
  override def stop(result: lang.Boolean): Unit = close()
  override def `end`(): Unit = close()
  override def `end`(endOptions: EndSpanOptions): Unit = close()

  override def record(note: Note[_]): Unit = ()
  override def startTimer(timerKey: String): Scope = () => ()
  override def stopTimer(timerKey: String): Unit = ()
  override def setAttribute(key: String, value: String): Unit = ()
  override def setAttribute(key: String, value: Long): Unit = ()
  override def setAttribute(key: String, value: Double): Unit = ()
  override def setAttribute(key: String, value: Boolean): Unit = ()
  override def setAttribute[T](key: AttributeKey[T], value: T): Unit = ()
  override def addEvent(name: String): Unit = ()
  override def addEvent(name: String, timestamp: Long): Unit = ()
  override def addEvent(name: String, attributes: Attributes): Unit = ()
  override def addEvent(name: String, attributes: Attributes, timestamp: Long): Unit = ()
  override def setStatus(canonicalCode: StatusCanonicalCode): Unit = ()
  override def setStatus(canonicalCode: StatusCanonicalCode, description: String): Unit = ()
  override def recordException(exception: Throwable): Unit = ()
  override def recordException(exception: Throwable, additionalAttributes: Attributes): Unit = ()
  override def updateName(name: String): Unit = ()
  // $COVERAGE-ON$
}
