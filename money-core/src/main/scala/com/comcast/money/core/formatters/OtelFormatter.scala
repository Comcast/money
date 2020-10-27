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

package com.comcast.money.core.formatters

import com.comcast.money.api.SpanId
import io.grpc.Context
import io.opentelemetry.common.{ AttributeKey, Attributes }
import io.opentelemetry.context.propagation.TextMapPropagator
import io.opentelemetry.trace._
import scala.collection.JavaConverters._

class OtelFormatter(propagator: TextMapPropagator) extends Formatter {
  def toHttpHeaders(spanId: SpanId, addHeader: (String, String) => Unit): Unit = {
    val context = TracingContextUtils.withSpan(PropagatingSpan(spanId), Context.ROOT)
    propagator.inject[Unit](context, (), (_, key, value) => addHeader(key, value))
  }

  def fromHttpHeaders(getHeader: String => String, log: String => Unit = _ => {}): Option[SpanId] = {
    val context = propagator.extract[Unit](Context.ROOT, (), (_, key) => getHeader(key))

    for {
      span <- Option(TracingContextUtils.getSpanWithoutDefault(context))
      spanContext = span.getContext
      if spanContext.isValid
      spanId = SpanId.fromSpanContext(spanContext)
    } yield spanId
  }

  override def fields: Seq[String] = propagator.fields.asScala.toSeq

  private case class PropagatingSpan(spanId: SpanId) extends Span {
    override def getContext: SpanContext = spanId.toSpanContext

    // $COVERAGE-OFF$
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
    override def `end`(): Unit = ()
    override def `end`(endOptions: EndSpanOptions): Unit = ()
    override def isRecording: Boolean = false
    // $COVERAGE-ON$
  }
}
