package com.comcast.money.otel.formatters

import com.comcast.money.core.formatters.OtelFormatter
import io.opentelemetry.extensions.trace.propagation.OtTracerPropagator

object LightstepFormatter {
  private[core] val TracerTraceIdHeader = "ot-tracer-traceid"
  private[core] val TracerSpanIdHeader = "ot-tracer-spanid"
  private[core] val TracerSampledHeader = "ot-tracer-sampled"
}

class LightstepFormatter extends OtelFormatter(OtTracerPropagator.getInstance)
