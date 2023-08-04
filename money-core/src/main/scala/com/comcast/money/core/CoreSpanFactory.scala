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

import com.comcast.money.api.{ InstrumentationLibrary, Resource, Span, SpanBuilder, SpanFactory, SpanHandler, SpanId }
import com.comcast.money.core.formatters.Formatter
import com.comcast.money.core.internal.SpanContext
import com.comcast.money.core.samplers.Sampler

private[core] final case class CoreSpanFactory(
  spanContext: SpanContext,
  clock: Clock,
  handler: SpanHandler,
  formatter: Formatter,
  sampler: Sampler,
  resource: Resource) extends SpanFactory {

  override def spanBuilder(spanName: String): SpanBuilder =
    spanBuilder(spanName, None, spanContext.current)

  override def newSpan(spanName: String): Span =
    spanBuilder(spanName, None, None).startSpan()

  override def newSpan(spanId: SpanId, spanName: String): Span =
    spanBuilder(spanName, Some(spanId), None).startSpan()

  override def childSpan(childName: String, span: Span): Span = childSpan(childName, span, sticky = true)

  override def childSpan(childName: String, span: Span, sticky: Boolean): Span =
    spanBuilder(childName, None, Some(span))
      .setSticky(true)
      .startSpan()

  private[core] def spanBuilder(spanName: String, spanId: Option[SpanId] = None, parentSpan: Option[Span] = spanContext.current): SpanBuilder =
    new CoreSpanBuilder(
      spanId = spanId,
      parentSpan = parentSpan,
      spanName = spanName,
      clock = clock,
      handler = handler,
      sampler = sampler,
      resource = resource)
}
