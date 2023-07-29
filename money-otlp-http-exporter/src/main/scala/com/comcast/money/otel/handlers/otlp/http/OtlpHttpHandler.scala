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

package com.comcast.money.otel.handlers.otlp.http

import com.comcast.money.otel.handlers.OtelSpanHandler
import com.typesafe.config.Config
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.sdk.trace.`export`.SpanExporter

import java.time.Duration

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
 *         class = "com.comcast.money.otel.handlers.otlp.http.OtlpHttpHandler"
 *         batch = true
 *         exporter-timeout-ms = 30000
 *         max-batch-size = 512
 *         max-queue-size = 2048
 *         schedule-delay-ms = 5000
 *         exporter {
 *           endpoint = "https://localhost:14250"
 *           compression = "gzip"
 *           timeout-ms = 1000
 *         }
 *       }
 *     ]
 *   }
 * }}}
 *
 */
class OtlpHttpHandler(config: Config) extends OtelSpanHandler(config) {
  override protected def createSpanExporter(config: Config): SpanExporter = {
    val builder = OtlpHttpSpanExporter.builder()

    val endpointKey = "endpoint"
    val compressionKey = "compression"
    val deadlineMillisKey = "timeout-ms"

    if (config.hasPath(endpointKey)) {
      builder.setEndpoint(config.getString(endpointKey))
    }
    if (config.hasPath(compressionKey)) {
      builder.setCompression(config.getString(compressionKey))
    }
    if (config.hasPath(deadlineMillisKey)) {
      builder.setTimeout(Duration.ofMillis(config.getLong(deadlineMillisKey)))
    }

    builder.build()
  }
}
