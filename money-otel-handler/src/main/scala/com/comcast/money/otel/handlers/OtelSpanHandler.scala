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
import com.comcast.money.core.handlers.ConfigurableHandler
import com.typesafe.config.Config
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo
import io.opentelemetry.sdk.trace.SpanProcessor
import io.opentelemetry.sdk.trace.`export`.{ BatchSpanProcessor, SimpleSpanProcessor, SpanExporter }

object OtelSpanHandler {
  val instrumentationLibraryInfo: InstrumentationLibraryInfo = InstrumentationLibraryInfo.create("money", "0.10.0")
}

abstract class OtelSpanHandler extends SpanHandler with ConfigurableHandler {

  private[otel] var processor: SpanProcessor = NoopSpanProcessor

  /**
   * Handles a span that has been stopped.
   *
   * @param span `SpanInfo` that contains the information for the completed span
   */
  override def handle(span: SpanInfo): Unit = {
    processor.onEnd(MoneyReadableSpanData(span))
  }

  override def configure(config: Config): Unit = {
    val spanExporter = createSpanExporter(config.atKey("exporter"))
    processor = createSpanProcessor(spanExporter, config)
  }

  def createSpanProcessor(spanExporter: SpanExporter, config: Config): SpanProcessor = {
    val batch = config.hasPath("batch") && config.getBoolean("batch")
    if (batch) {
      configureBatchProcessor(spanExporter, config)
    } else {
      configureSimpleProcessor(spanExporter, config)
    }
  }

  private def configureSimpleProcessor(spanExporter: SpanExporter, config: Config): SpanProcessor = {
    var builder = SimpleSpanProcessor.newBuilder(spanExporter)

    val exportOnlySampledKey = "export-only-sampled"

    if (config.hasPath(exportOnlySampledKey)) {
      builder = builder.setExportOnlySampled(config.getBoolean(exportOnlySampledKey))
    }

    builder.build()
  }

  private def configureBatchProcessor(spanExporter: SpanExporter, config: Config): SpanProcessor = {
    var builder = BatchSpanProcessor.newBuilder(spanExporter)

    val exportOnlySampledKey = "export-only-sampled"
    val exporterTimeoutMillisKey = "exporter-timeout-ms"
    val maxExportBatchSizeKey = "max-batch-size"
    val maxQueueSizeKey = "max-queue-size"
    val scheduleDelayMillisKey = "schedule-delay-ms"

    if (config.hasPath(exportOnlySampledKey)) {
      builder = builder.setExportOnlySampled(config.getBoolean(exportOnlySampledKey))
    }
    if (config.hasPath(exporterTimeoutMillisKey)) {
      builder = builder.setExporterTimeoutMillis(config.getInt(exporterTimeoutMillisKey))
    }
    if (config.hasPath(maxExportBatchSizeKey)) {
      builder = builder.setMaxExportBatchSize(config.getInt(maxExportBatchSizeKey))
    }
    if (config.hasPath(maxQueueSizeKey)) {
      builder = builder.setMaxQueueSize(config.getInt(maxQueueSizeKey))
    }
    if (config.hasPath(scheduleDelayMillisKey)) {
      builder = builder.setScheduleDelayMillis(config.getLong(scheduleDelayMillisKey))
    }

    builder.build()
  }

  def createSpanExporter(config: Config): SpanExporter
}