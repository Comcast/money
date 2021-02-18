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

package com.comcast.money.otel.formatters

import com.comcast.money.core.formatters.OtelFormatter
import com.comcast.money.otel.formatters.B3MultiHeaderFormatter.{ B3ParentSpanIdHeader, B3SampledHeader, B3SpanIdHeader, B3TraceIdHeader }
import io.opentelemetry.`extension`.trace.propagation.B3Propagator

object B3MultiHeaderFormatter extends OtelFormatter(B3Propagator.injectingMultiHeaders) {
  private[formatters] val B3TraceIdHeader = "X-B3-TraceId"
  private[formatters] val B3SpanIdHeader = "X-B3-SpanId"
  private[formatters] val B3ParentSpanIdHeader = "X-B3-ParentSpanId"
  private[formatters] val B3SampledHeader = "X-B3-Sampled"

  override def fields: Seq[String] = Seq(B3TraceIdHeader, B3SpanIdHeader, B3ParentSpanIdHeader, B3SampledHeader)
}