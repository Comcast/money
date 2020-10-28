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
import io.opentelemetry.api.common.{ AttributeKey, Attributes }
import io.opentelemetry.context.Context
import io.opentelemetry.api.trace.{ SpanContext, Span => OtelSpan }

private[core] class CoreSpanBuilder(
  var parentSpan: Option[Span],
  spanName: String,
  spanFactory: SpanFactory) extends Span.Builder {

  var sticky: Boolean = true
  var spanKind: OtelSpan.Kind = OtelSpan.Kind.INTERNAL
  var startTimeNanos: Long = 0
  var notes: List[Note[_]] = List()

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

  override def addLink(spanContext: SpanContext): Span.Builder = this

  override def addLink(spanContext: SpanContext, attributes: Attributes): Span.Builder = this

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

  override def startSpan(): Span = {
    val newSpan = parentSpan match {
      case Some(s) => spanFactory.childSpan(spanName, s, sticky)
      case None => spanFactory.newSpan(spanName)
    }

    if (spanKind != OtelSpan.Kind.INTERNAL) {
      newSpan.updateKind(spanKind)
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
