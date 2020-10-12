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

import java.util.Collections
import java.util.concurrent.TimeUnit

import com.comcast.money.api.{ Event, Note, SpanId, SpanInfo }
import io.opentelemetry.trace.{ Span, StatusCanonicalCode }

case class CoreSpanInfo(
  id: SpanId,
  name: String,
  kind: Span.Kind = Span.Kind.INTERNAL,
  startTimeNanos: Long = 0L,
  endTimeNanos: Long = 0L,
  durationNanos: Long = 0L,
  status: StatusCanonicalCode = StatusCanonicalCode.UNSET,
  description: String = "",
  notes: java.util.Map[String, Note[_]] = Collections.emptyMap(),
  events: java.util.List[Event] = Collections.emptyList(),
  appName: String = Money.Environment.applicationName,
  host: String = Money.Environment.hostName) extends SpanInfo {

  override def isRecording: Boolean = startTimeNanos > 0L && endTimeNanos <= 0L
  override def startTimeMillis: Long = toMillis(startTimeNanos)
  override def startTimeMicros: Long = toMicros(startTimeNanos)
  override def endTimeMillis: Long = toMillis(endTimeNanos)
  override def endTimeMicros: Long = toMicros(endTimeNanos)
  override def durationMicros: Long = toMicros(durationNanos)
  override def success: java.lang.Boolean = status match {
    case StatusCanonicalCode.OK if endTimeNanos > 0L => true
    case StatusCanonicalCode.ERROR if endTimeNanos > 0L => false
    case _ => null
  }

  private def toMillis(nanos: Long): Long = TimeUnit.NANOSECONDS.toMillis(nanos)
  private def toMicros(nanos: Long): Long = TimeUnit.NANOSECONDS.toMicros(nanos)
}
