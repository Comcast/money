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
import io.opentelemetry.`extension`.trace.propagation.OtTracerPropagator

object LightstepFormatter {
  private[formatters] val TracerTraceIdHeader = "ot-tracer-traceid"
  private[formatters] val TracerSpanIdHeader = "ot-tracer-spanid"
  private[formatters] val TracerSampledHeader = "ot-tracer-sampled"
}

final class LightstepFormatter extends OtelFormatter(OtTracerPropagator.getInstance)
