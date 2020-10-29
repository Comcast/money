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

import com.comcast.money.api.{ InstrumentationLibrary, Span, SpanFactory, SpanHandler, SpanId }
import com.comcast.money.core.formatters.Formatter
import com.comcast.money.core.samplers.{ DropResult, RecordResult, Sampler }
import io.grpc.Context
import io.opentelemetry.trace.TraceFlags

import scala.collection.JavaConverters._

private[core] final case class CoreSpanFactory(
  clock: Clock,
  handler: SpanHandler,
  formatter: Formatter,
  sampler: Sampler,
  library: InstrumentationLibrary) extends SpanFactory {

  override def spanBuilder(spanName: String): Span.Builder =
    new CoreSpanBuilder(None, spanName, this)
      .setParent(Context.current)

  override def newSpan(spanName: String): Span = newSpan(SpanId.createNew(), spanName)

  override def newSpan(spanId: SpanId, spanName: String): Span = createNewSpan(spanId, None, spanName)

  override def childSpan(childName: String, span: Span): Span = childSpan(childName, span, sticky = true)

  override def childSpan(childName: String, span: Span, sticky: Boolean): Span = {
    val info = span.info
    val parentSpanId = info.id
    val spanId = parentSpanId.createChild()
    val child = createNewSpan(spanId, Some(parentSpanId), childName)

    if (sticky) {
      info.notes.values.asScala
        .filter(_.isSticky)
        .foreach(child.record)
    }

    child
  }

  private def createNewSpan(spanId: SpanId, parentSpanId: Option[SpanId], spanName: String): Span =
    sampler.shouldSample(spanId, parentSpanId, spanName) match {
      case DropResult => UnrecordedSpan(spanId.withTraceFlags(TraceFlags.getDefault), spanName)
      case RecordResult(sample, notes) =>
        val flags = if (sample) TraceFlags.getSampled else TraceFlags.getDefault
        val span = CoreSpan(
          id = spanId.withTraceFlags(flags),
          name = spanName,
          library = library,
          clock = clock,
          handler = handler)
        notes.foreach { span.record }
        span
    }
}
