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

package com.comcast.money.otel.handlers.jaeger

import com.comcast.money.otel.handlers.OtelSpanHandler
import com.typesafe.config.Config
import io.opentelemetry.exporters.jaeger.JaegerGrpcSpanExporter
import io.opentelemetry.sdk.trace.`export`.SpanExporter

class JaegerOtelSpanHandler extends OtelSpanHandler {
  override protected def createSpanExporter(config: Config): SpanExporter = {
    val builder = JaegerGrpcSpanExporter.newBuilder()

    val serviceNameKey = "service-name"
    val endpointKey = "endpoint"
    val deadlineMillisKey = "deadline-ms"

    builder.setServiceName(config.getString(serviceNameKey))
    if (config.hasPath(endpointKey)) {
      builder.setEndpoint(config.getString(endpointKey))
    }
    if (config.hasPath(deadlineMillisKey)) {
      builder.setDeadlineMs(config.getLong(deadlineMillisKey))
    }

    builder.build()
  }
}
