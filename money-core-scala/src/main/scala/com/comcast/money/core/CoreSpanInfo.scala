/*
 * Copyright 2012-2015 Comcast Cable Communications Management, LLC
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

import com.comcast.money.api.{ Note, SpanId, SpanInfo }

case class CoreSpanInfo(
  id: SpanId,
  name: String,
  startTimeMillis: java.lang.Long = 0L,
  startTimeMicros: java.lang.Long = 0L,
  endTimeMillis: java.lang.Long = 0L,
  endTimeMicros: java.lang.Long = 0L,
  durationMicros: java.lang.Long = 0L,
  success: java.lang.Boolean = true,
  notes: java.util.Map[String, Note[_]] = new java.util.HashMap[String, Note[_]](),
  appName: String = Money.Environment.applicationName,
  host: String = Money.Environment.hostName
) extends SpanInfo
