package com.comcast.money.otel.handlers.jaeger

import com.comcast.money.otel.handlers.OtelSpanHandler
import com.typesafe.config.Config
import io.opentelemetry.exporters.jaeger.JaegerGrpcSpanExporter
import io.opentelemetry.sdk.trace.`export`.SpanExporter

class JaegerOtelSpanHandler extends OtelSpanHandler {
  override def createSpanExporter(config: Config): SpanExporter = {
    var builder = JaegerGrpcSpanExporter.newBuilder()

    val deadlineMillisKey = "deadline-ms"
    val endpointKey = "endpoint"
    val serviceNameKey = "service-name"

    if (config.hasPath(deadlineMillisKey)) {
      builder = builder.setDeadlineMs(config.getLong(deadlineMillisKey))
    }
    if (config.hasPath(endpointKey)) {
      builder = builder.setEndpoint(config.getString(endpointKey))
    }
    if (config.hasPath(serviceNameKey)) {
      builder = builder.setServiceName(config.getString(serviceNameKey))
    }

    builder.build()
  }
}
