package com.comcast.money.otel.formatters

import com.comcast.money.core.formatters.OtelFormatter
import com.comcast.money.otel.formatters.B3MultiHeaderFormatter.{B3ParentSpanIdHeader, B3SampledHeader, B3SpanIdHeader, B3TraceIdHeader}
import io.opentelemetry.extensions.trace.propagation.B3Propagator

object B3MultiHeaderFormatter {
  private[core] val B3TraceIdHeader = "X-B3-TraceId"
  private[core] val B3SpanIdHeader = "X-B3-SpanId"
  private[core] val B3ParentSpanIdHeader = "X-B3-ParentSpanId"
  private[core] val B3SampledHeader = "X-B3-Sampled"
}

final class B3MultiHeaderFormatter extends OtelFormatter(B3Propagator.getMultipleHeaderPropagator) {
  override def fields: Seq[String] = Seq(B3TraceIdHeader, B3SpanIdHeader, B3ParentSpanIdHeader, B3SampledHeader)
}
