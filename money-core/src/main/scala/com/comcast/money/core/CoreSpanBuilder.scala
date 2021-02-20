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
import com.comcast.money.core.internal.{ SpanContext, SpanLocal }
import com.comcast.money.core.samplers.{ DropResult, RecordResult, Sampler }
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.{ SpanKind, TraceFlags, SpanContext => OtelSpanContext }
import io.opentelemetry.context.Context

import java.util.concurrent.TimeUnit
import scala.collection.JavaConverters._

private[core] class CoreSpanBuilder(
  spanId: Option[SpanId],
  var parentContext: Context = Context.current(),
  spanContext: SpanContext = SpanLocal,
  spanName: String,
  clock: Clock,
  handler: SpanHandler,
  sampler: Sampler,
  library: InstrumentationLibrary) extends SpanBuilder {

  var sticky: Boolean = true
  var spanKind: SpanKind = SpanKind.INTERNAL
  var startTimeNanos: Long = 0L
  var notes: List[Note[_]] = List()
  var links: List[LinkInfo] = List()

  override def setParent(context: Context): SpanBuilder = {
    parentContext = context
    this
  }

  override def setSticky(sticky: Boolean): SpanBuilder = {
    this.sticky = sticky
    this
  }

  override def setNoParent(): SpanBuilder = {
    parentContext = Context.root
    this
  }

  override def addLink(spanContext: OtelSpanContext, attributes: Attributes): SpanBuilder = {
    links = new CoreLink(spanContext, attributes) :: links
    this
  }

  override def record(note: Note[_]): SpanBuilder = {
    notes = note :: notes
    this
  }

  override def setSpanKind(spanKind: SpanKind): SpanBuilder = {
    this.spanKind = spanKind
    this
  }

  override def setStartTimestamp(startTimestamp: Long, timeUnit: TimeUnit): SpanBuilder = {
    this.startTimeNanos = timeUnit.toNanos(startTimestamp)
    this
  }

  private[core] def createSpan(id: SpanId, name: String, kind: SpanKind, startTimeNanos: Long): Span = CoreSpan(
    id = id,
    name = name,
    startTimeNanos = startTimeNanos,
    kind = kind,
    links = links,
    library = library,
    clock = clock,
    handler = handler)

  override def startSpan(): Span = {
    val parentSpan = spanContext.fromContext(parentContext)
    val parentSpanId = parentSpan.map { _.info.id }

    val spanId = (this.spanId, parentSpanId) match {
      case (Some(id), _) => id
      case (None, Some(id)) => id.createChild()
      case _ => SpanId.createNew()
    }

    sampler.shouldSample(spanId, parentSpanId, spanName) match {
      case DropResult => UnrecordedSpan(spanId, spanName)
      case RecordResult(sample, notes) =>
        val traceFlags = if (sample) TraceFlags.getSampled else TraceFlags.getDefault

        val span = createSpan(
          id = spanId.withTraceFlags(traceFlags),
          name = spanName,
          startTimeNanos = if (startTimeNanos > 0L) startTimeNanos else clock.now,
          kind = spanKind)

        // propagate parent span notes
        parentSpan match {
          case Some(ps) if sticky =>
            ps.info.notes.values.asScala
              .filter { _.isSticky }
              .foreach { span.record }
          case _ =>
        }
        // add sampler notes
        notes.foreach { span.record }
        // add builder notes
        this.notes.foreach { span.record }

        span
    }
  }
}
