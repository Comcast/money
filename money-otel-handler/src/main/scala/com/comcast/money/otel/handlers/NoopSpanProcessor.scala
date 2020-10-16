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

package com.comcast.money.otel.handlers

import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.{ ReadWriteSpan, ReadableSpan, SpanProcessor }

// $COVERAGE-OFF
private[otel] object NoopSpanProcessor extends SpanProcessor {
  override def onStart(span: ReadWriteSpan): Unit = ()
  override def isStartRequired: Boolean = false
  override def onEnd(span: ReadableSpan): Unit = ()
  override def isEndRequired: Boolean = false
  override def shutdown(): CompletableResultCode = CompletableResultCode.ofSuccess()
  override def forceFlush(): CompletableResultCode = CompletableResultCode.ofSuccess()
}
// $COVERAGE-ON
