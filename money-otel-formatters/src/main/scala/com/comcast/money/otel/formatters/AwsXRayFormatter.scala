package com.comcast.money.otel.formatters

import com.comcast.money.core.formatters.OtelFormatter
import io.opentelemetry.extensions.trace.propagation.AwsXRayPropagator

object AwsXRayFormatter {
  private[core] val AmznTraceIdHeader = "X-Amzn-Trace-Id"
}

final class AwsXRayFormatter extends OtelFormatter(AwsXRayPropagator.getInstance)
