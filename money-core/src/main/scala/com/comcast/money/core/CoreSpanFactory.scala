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

import java.util.function

import com.comcast.money.api.{ InstrumentationLibrary, Sampler, Span, SpanFactory, SpanHandler, SpanId }
import com.comcast.money.core.formatters.Formatter
import io.opentelemetry.trace.TraceFlags
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

private[core] final case class CoreSpanFactory(
  clock: Clock,
  handler: SpanHandler,
  formatter: Formatter,
  sampler: Sampler,
  library: InstrumentationLibrary) extends SpanFactory {

  private val logger = LoggerFactory.getLogger(classOf[CoreSpanFactory])

  override def newSpan(spanName: String): Span = newSpan(SpanId.createNew(), spanName)

  override def newSpan(spanId: SpanId, spanName: String): Span = createNewSpan(spanId, None, spanName)

  /**
   * Continues a trace by creating a child span from the given x-moneytrace header
   * value or a root span if header is malformed.
   *
   * @param childName - the name of the child span to create
   * @param getHeader - function for retrieving value of x-moneytrace header
   * @return a child span with trace id and parent id from trace context header or a new root span if the
   * traceContextHeader is malformed.
   */
  override def newSpanFromHeader(childName: String, getHeader: function.Function[String, String]): Span =
    formatter.fromHttpHeaders(getHeader.apply, logger.warn) match {
      case Some(spanId) => newSpan(spanId.createChild(), childName)
      case None =>
        logger.warn(s"creating root span because http header '${getHeader}' was malformed")
        newSpan(childName)
    }

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
      case Sampler.Decision.DROP => UnrecordedSpan(spanId.withTraceFlags(TraceFlags.getDefault), spanName)
      case Sampler.Decision.RECORD =>
        CoreSpan(
          id = spanId.withTraceFlags(TraceFlags.getDefault),
          name = spanName,
          library = library,
          clock = clock,
          handler = handler)
      case Sampler.Decision.SAMPLE_AND_RECORD =>
        CoreSpan(
          id = spanId.withTraceFlags(TraceFlags.getSampled),
          name = spanName,
          library = library,
          clock = clock,
          handler = handler)
    }
}
