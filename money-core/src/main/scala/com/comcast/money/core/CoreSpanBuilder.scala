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

import java.util.concurrent.TimeUnit

import com.comcast.money.api.{ Note, Span, SpanFactory }
import io.grpc.Context
import io.opentelemetry.common.{ AttributeKey, Attributes }
import io.opentelemetry.trace.{ SpanContext, TracingContextUtils, Span => OtelSpan }

class CoreSpanBuilder(
  var parentSpan: Option[Span],
  sticky: Boolean,
  spanName: String,
  spanFactory: SpanFactory) extends Span.Builder {

  var spanKind: OtelSpan.Kind = OtelSpan.Kind.INTERNAL
  var startTimeNanos: Long = 0
  var notes: List[Note[_]] = List()

  override def setParent(context: Context): Span.Builder = {
    parentSpan = Option(context)
      .map(TracingContextUtils.getSpanWithoutDefault)
      .flatMap {
        case span: Span => Some(span)
        case _ => None
      }
    this
  }

  override def setNoParent(): Span.Builder = {
    parentSpan = None
    this
  }

  override def addLink(spanContext: SpanContext): Span.Builder = this

  override def addLink(spanContext: SpanContext, attributes: Attributes): Span.Builder = this

  override def setAttribute(key: String, value: String): Span.Builder = setAttribute[String](AttributeKey.stringKey(key), value)

  override def setAttribute(key: String, value: Long): Span.Builder = setAttribute[java.lang.Long](AttributeKey.longKey(key), value)

  override def setAttribute(key: String, value: Double): Span.Builder = setAttribute[java.lang.Double](AttributeKey.doubleKey(key), value)

  override def setAttribute(key: String, value: Boolean): Span.Builder = setAttribute[java.lang.Boolean](AttributeKey.booleanKey(key), value)

  override def setAttribute[T](key: AttributeKey[T], value: T): Span.Builder = {
    notes = Note.of(key, value) :: notes
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

  override def startSpan(): Span = {
    val newSpan = parentSpan match {
      case Some(span) => spanFactory.childSpan(spanName, span, sticky)
      case None => spanFactory.newSpan(spanName)
    }

    if (spanKind != OtelSpan.Kind.INTERNAL) {
      newSpan.setAttribute("kind", spanKind.name)
    }
    notes.foreach { newSpan.record }

    if (startTimeNanos <= 0) {
      newSpan.start()
    } else {
      val startTimeSeconds = TimeUnit.NANOSECONDS.toSeconds(startTimeNanos)
      val nanoAdjustment = (startTimeNanos - TimeUnit.SECONDS.toNanos(startTimeSeconds)).toInt
      newSpan.start(startTimeSeconds, nanoAdjustment)
    }
    newSpan
  }
}
