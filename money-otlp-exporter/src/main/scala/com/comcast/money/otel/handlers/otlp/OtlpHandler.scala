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

package com.comcast.money.otel.handlers.otlp

import com.comcast.money.otel.handlers.OtelSpanHandler
import com.typesafe.config.Config
import io.opentelemetry.exporters.otlp.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.trace.`export`.SpanExporter

/**
 * A Money [[com.comcast.money.api.SpanHandler]] that can export spans to OTLP Collector
 * through the OpenTelemetry OTLP `SpanExporter`.
 *
 * Sample configuration:
 *
 * {{{
 *   handling = {
 *     async = true
 *     handlers = [
 *       {
 *         class = "com.comcast.money.otel.handlers.otlp.OtlpHandler"
 *         batch = true
 *         export-only-sampled = true
 *         exporter-timeout-ms = 30000
 *         max-batch-size = 512
 *         max-queue-size = 2048
 *         schedule-delay-ms = 5000
 *         exporter {
 *           endpoint = "localhost:14250"
 *           deadline-ms = 1000
 *           use-tls = true
 *         }
 *       }
 *     ]
 *   }
 * }}}
 *
 */
class OtlpHandler extends OtelSpanHandler {
  override protected def createSpanExporter(config: Config): SpanExporter = {
    val builder = OtlpGrpcSpanExporter.newBuilder()

    val endpointKey = "endpoint"
    val deadlineMillisKey = "deadline-ms"
    val useTlsKey = "use-tls"

    if (config.hasPath(endpointKey)) {
      builder.setEndpoint(config.getString(endpointKey))
    }
    if (config.hasPath(deadlineMillisKey)) {
      builder.setDeadlineMs(config.getLong(deadlineMillisKey))
    }
    if (config.hasPath(useTlsKey)) {
      builder.setUseTls(config.getBoolean(useTlsKey))
    }

    builder.build()
  }
}
