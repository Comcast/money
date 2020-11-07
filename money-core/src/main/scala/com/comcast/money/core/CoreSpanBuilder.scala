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

import com.comcast.money.api.{ InstrumentationLibrary, Note, Span, SpanHandler, SpanId, SpanInfo }
import com.comcast.money.core.samplers.{ DropResult, RecordResult, Sampler }
import io.opentelemetry.api.common.{ AttributeKey, Attributes }
import io.opentelemetry.context.Context
import io.opentelemetry.api.trace.{ SpanContext, TraceFlags, Span => OtelSpan }

import scala.collection.JavaConverters._

private[core] class CoreSpanBuilder(
  spanId: Option[SpanId],
  var parentSpan: Option[Span],
  spanName: String,
  clock: Clock,
  handler: SpanHandler,
  sampler: Sampler,
  library: InstrumentationLibrary) extends Span.Builder {

  var sticky: Boolean = true
  var spanKind: OtelSpan.Kind = OtelSpan.Kind.INTERNAL
  var startTimeNanos: Long = 0L
  var notes: List[Note[_]] = List()
  var links: List[SpanInfo.Link] = List()

  override def setParent(context: Context): Span.Builder = {
    parentSpan = Option(context)
      .flatMap { ctx => Option(OtelSpan.fromContextOrNull(ctx)) }
      .flatMap {
        case span: Span => Some(span)
        case _ => None
      }
    this
  }

  override def setParent(span: Span): Span.Builder = {
    parentSpan = Option(span)
    this
  }

  override def setParent(span: Option[Span]): Span.Builder = {
    parentSpan = span
    this
  }

  override def setSticky(sticky: Boolean): Span.Builder = {
    this.sticky = sticky
    this
  }

  override def setNoParent(): Span.Builder = {
    parentSpan = None
    this
  }

  override def addLink(spanContext: SpanContext): Span.Builder = addLink(spanContext, Attributes.empty)

  override def addLink(spanContext: SpanContext, attributes: Attributes): Span.Builder = {
    links = CoreLink(spanContext, attributes) :: links
    this
  }

  override def setAttribute(key: String, value: String): Span.Builder = setAttribute[String](AttributeKey.stringKey(key), value)

  override def setAttribute(key: String, value: Long): Span.Builder = setAttribute[java.lang.Long](AttributeKey.longKey(key), value)

  override def setAttribute(key: String, value: Double): Span.Builder = setAttribute[java.lang.Double](AttributeKey.doubleKey(key), value)

  override def setAttribute(key: String, value: Boolean): Span.Builder = setAttribute[java.lang.Boolean](AttributeKey.booleanKey(key), value)

  override def setAttribute[T](key: AttributeKey[T], value: T): Span.Builder = record(Note.of(key, value))

  override def record(note: Note[_]): Span.Builder = {
    notes = note :: notes
    this
  }

  override def setSpanKind(spanKind: OtelSpan.Kind): Span.Builder = {
    this.spanKind = spanKind
    this
  }

  override def setStartTimestamp(startTimestamp: Long): Span.Builder = {
    this.startTimeNanos = startTimestamp
    this
  }

  private[core] def createSpan(id: SpanId, name: String, kind: OtelSpan.Kind, startTimeNanos: Long): Span = CoreSpan(
    id = id,
    name = name,
    startTimeNanos = startTimeNanos,
    kind = kind,
    links = links,
    library = library,
    clock = clock,
    handler = handler)

  override def startSpan(): Span = {
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
          startTimeNanos = if (startTimeNanos > 0) startTimeNanos else clock.now,
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
