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

package com.comcast.money.otel.handlers

import com.comcast.money.api.{ SpanHandler, SpanInfo }
import com.typesafe.config.{ Config, ConfigFactory }
import io.opentelemetry.sdk.trace.SpanProcessor
import io.opentelemetry.sdk.trace.`export`.{ BatchSpanProcessor, SimpleSpanProcessor, SpanExporter }

/**
 * An abstract `SpanHandler` that can wrap an OpenTelemetry `SpanExporter` implementation
 * and export spans to an OpenTelemetry-compatible exporter such as ZipKin or Jaeger.
 */
abstract class OtelSpanHandler(config: Config) extends SpanHandler {

  private[otel] val exporter: SpanExporter = createSpanExporter(getExporterConfig(config))
  private[otel] val processor: SpanProcessor = createSpanProcessor(exporter, config)

  /**
   * Handles a span that has been stopped.
   *
   * @param span `SpanInfo` that contains the information for the completed span
   */
  override def handle(span: SpanInfo): Unit = {
    processor.onEnd(new MoneyReadableSpanData(span))
  }

  protected def createSpanProcessor(spanExporter: SpanExporter, config: Config): SpanProcessor = {
    val batch = config.hasPath("batch") && config.getBoolean("batch")
    if (batch) {
      configureBatchProcessor(spanExporter, config)
    } else {
      configureSimpleProcessor(spanExporter, config)
    }
  }

  private def configureSimpleProcessor(spanExporter: SpanExporter, config: Config): SpanProcessor = {
    val builder = SimpleSpanProcessor.builder(spanExporter)

    val exportOnlySampledKey = "export-only-sampled"

    if (config.hasPath(exportOnlySampledKey)) {
      builder.setExportOnlySampled(config.getBoolean(exportOnlySampledKey))
    }

    builder.build()
  }

  private def configureBatchProcessor(spanExporter: SpanExporter, config: Config): SpanProcessor = {
    val builder = BatchSpanProcessor.builder(spanExporter)

    val exportOnlySampledKey = "export-only-sampled"
    val exporterTimeoutMillisKey = "exporter-timeout-ms"
    val maxExportBatchSizeKey = "max-batch-size"
    val maxQueueSizeKey = "max-queue-size"
    val scheduleDelayMillisKey = "schedule-delay-ms"

    if (config.hasPath(exportOnlySampledKey)) {
      builder.setExportOnlySampled(config.getBoolean(exportOnlySampledKey))
    }
    if (config.hasPath(exporterTimeoutMillisKey)) {
      builder.setExporterTimeoutMillis(config.getInt(exporterTimeoutMillisKey))
    }
    if (config.hasPath(maxExportBatchSizeKey)) {
      builder.setMaxExportBatchSize(config.getInt(maxExportBatchSizeKey))
    }
    if (config.hasPath(maxQueueSizeKey)) {
      builder.setMaxQueueSize(config.getInt(maxQueueSizeKey))
    }
    if (config.hasPath(scheduleDelayMillisKey)) {
      builder.setScheduleDelayMillis(config.getLong(scheduleDelayMillisKey))
    }

    builder.build()
  }

  protected def getExporterConfig(config: Config): Config =
    if (config.hasPath("exporter")) {
      config.getConfig("exporter")
    } else ConfigFactory.empty

  protected def createSpanExporter(config: Config): SpanExporter
}