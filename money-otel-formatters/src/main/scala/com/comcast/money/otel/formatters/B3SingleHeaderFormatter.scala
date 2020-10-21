package com.comcast.money.otel.formatters

import com.comcast.money.core.formatters.OtelFormatter
import com.comcast.money.otel.formatters.B3SingleHeaderFormatter.B3Header
import io.opentelemetry.extensions.trace.propagation.B3Propagator

object B3SingleHeaderFormatter {
  private[formatters] val B3Header = "b3"
}

final class B3SingleHeaderFormatter extends OtelFormatter(B3Propagator.getSingleHeaderPropagator) {
  override def fields: Seq[String] = Seq(B3Header)
}
