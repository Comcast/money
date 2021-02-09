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
import com.comcast.money.api.{ InstrumentationLibrary, Note, SpanId, SpanInfo }
import io.opentelemetry.api.trace.{ Span, SpanKind, StatusCode }

private[core] case class CoreSpanInfo(
  id: SpanId,
  name: String,
  kind: SpanKind = SpanKind.INTERNAL,
  startTimeNanos: Long = 0L,
  endTimeNanos: Long = 0L,
  durationNanos: Long = 0L,
  status: StatusCode = StatusCode.UNSET,
  description: String = "",
  notes: java.util.Map[String, Note[_]] = Collections.emptyMap(),
  override val events: java.util.List[SpanInfo.Event] = Collections.emptyList(),
  override val links: java.util.List[SpanInfo.Link] = Collections.emptyList(),
  library: InstrumentationLibrary = Money.InstrumentationLibrary,
  appName: String = Money.Environment.applicationName,
  host: String = Money.Environment.hostName) extends SpanInfo