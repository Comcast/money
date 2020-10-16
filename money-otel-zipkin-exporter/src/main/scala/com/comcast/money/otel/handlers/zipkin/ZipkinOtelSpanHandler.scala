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

package com.comcast.money.otel.handlers.zipkin

import com.comcast.money.otel.handlers.OtelSpanHandler
import com.typesafe.config.Config
import io.opentelemetry.exporters.zipkin.ZipkinSpanExporter
import io.opentelemetry.sdk.trace.`export`.SpanExporter
import zipkin2.codec.SpanBytesEncoder

class ZipkinOtelSpanHandler extends OtelSpanHandler {
  override protected def createSpanExporter(config: Config): SpanExporter = {
    val builder = ZipkinSpanExporter.newBuilder()

    val serviceNameKey = "service-name"
    val endpointKey = "endpoint"
    val encoderKey = "encoder"

    if (config.hasPath(serviceNameKey)) {
      builder.setServiceName(config.getString(serviceNameKey))
    }
    if (config.hasPath(endpointKey)) {
      builder.setEndpoint(config.getString(endpointKey))
    }
    if (config.hasPath(encoderKey)) {
      val encoder = config.getString(encoderKey) match {
        case "json-v1" => SpanBytesEncoder.JSON_V1
        case "thrift" => SpanBytesEncoder.THRIFT
        case "json-v2" => SpanBytesEncoder.JSON_V2
        case "proto3" => SpanBytesEncoder.PROTO3
      }
      builder.setEncoder(encoder)
    }

    builder.build()
  }
}
