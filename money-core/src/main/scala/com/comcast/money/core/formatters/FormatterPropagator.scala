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

import java.util

import com.comcast.money.api.SpanId
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapPropagator
import io.opentelemetry.trace.{ Span, TracingContextUtils }

import scala.collection.JavaConverters._

final case class FormatterPropagator(formatter: Formatter) extends TextMapPropagator {

  override def inject[C](context: Context, carrier: C, setter: TextMapPropagator.Setter[C]): Unit =
    Option(Span.fromContextOrNull(context))
      .map { _.getSpanContext }
      .map { SpanId.fromSpanContext }
      .filter { _.isValid }
      .foreach {
        spanId => formatter.toHttpHeaders(spanId, (key, value) => setter.set(carrier, key, value))
      }

  override def extract[C](context: Context, carrier: C, getter: TextMapPropagator.Getter[C]): Context =
    formatter.fromHttpHeaders(key => getter.get(carrier, key))
      .filter { _.isValid }
      .map { _.toSpanContext() }
      .map { Span.wrap }
      .map { _.storeInContext(context) }
      .getOrElse(context)

  override def fields(): util.List[String] = formatter.fields.asJava
}