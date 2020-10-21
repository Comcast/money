package com.comcast.money.otel.formatters

import com.comcast.money.core.formatters.OtelFormatter
import io.opentelemetry.extensions.trace.propagation.OtTracerPropagator

object LightstepFormatter {
  private[formatters] val TracerTraceIdHeader = "ot-tracer-traceid"
  private[formatters] val TracerSpanIdHeader = "ot-tracer-spanid"
  private[formatters] val TracerSampledHeader = "ot-tracer-sampled"
}

final class LightstepFormatter extends OtelFormatter(OtTracerPropagator.getInstance)
