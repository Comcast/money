package com.comcast.money.otel.formatters

import com.comcast.money.core.formatters.OtelFormatter
import io.opentelemetry.extensions.trace.propagation.JaegerPropagator

object JaegerFormatter {
  private[core] val UberTraceIdHeader = "uber-trace-id"
}

class JaegerFormatter extends OtelFormatter(JaegerPropagator.getInstance)
