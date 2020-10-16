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

package com.comcast.money.otel

import com.comcast.money.api.{SpanHandler, SpanInfo}
import com.comcast.money.core.handlers.ConfigurableHandler
import com.typesafe.config.Config
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo
import io.opentelemetry.sdk.trace.SpanProcessor
import io.opentelemetry.sdk.trace.`export`.{BatchSpanProcessor, SimpleSpanProcessor, SpanExporter}

object OtelSpanHandler {
  val instrumentationLibraryInfo: InstrumentationLibraryInfo = InstrumentationLibraryInfo.create("money", "0.10.0")
}

abstract class OtelSpanHandler extends SpanHandler with ConfigurableHandler {

  var processor: SpanProcessor = NoopSpanProcessor

  /**
   * Handles a span that has been stopped.
   *
   * @param span `SpanInfo` that contains the information for the completed span
   */
  override def handle(span: SpanInfo): Unit = {
    processor.onEnd(MoneyReadableSpanData(span))
  }

  override def configure(config: Config): Unit = {

    val exporterConfig = config.atKey("exporter")
    val batch = config.hasPath("batch") && config.getBoolean("batch")
    processor = if (batch) {
      configureBatchProcessor(config) { createExporter(exporterConfig) }
    } else {
      configureSimpleProcessor(config) { createExporter(exporterConfig) }
    }
  }

  private def configureSimpleProcessor(config: Config)(createExporter: => SpanExporter): SpanProcessor = {
    var builder = SimpleSpanProcessor.newBuilder(createExporter)

    val exportOnlySampled = "export-only-sampled"

    if (config.hasPath(exportOnlySampled)) {
      builder = builder.setExportOnlySampled(config.getBoolean(exportOnlySampled))
    }

    builder.build()
  }

  private def configureBatchProcessor(config: Config)(createExporter: => SpanExporter): SpanProcessor = {
    var builder = BatchSpanProcessor.newBuilder(createExporter)

    val exportOnlySampled = "export-only-sampled"
    val exporterTimeoutMillis = "exporter-timeout-ms"
    val maxExportBatchSize = "max-batch-size"
    val maxQueueSize = "max-queue-size"
    val scheduleDelayMills = "schedule-delay-ms"

    if (config.hasPath(exportOnlySampled)) {
      builder = builder.setExportOnlySampled(config.getBoolean(exportOnlySampled))
    }
    if (config.hasPath(exporterTimeoutMillis)) {
      builder = builder.setExporterTimeoutMillis(config.getInt(exporterTimeoutMillis))
    }
    if (config.hasPath(maxExportBatchSize)) {
      builder = builder.setMaxExportBatchSize(config.getInt(maxExportBatchSize))
    }
    if (config.hasPath(maxQueueSize)) {
      builder = builder.setMaxQueueSize(config.getInt(maxQueueSize))
    }
    if (config.hasPath(scheduleDelayMills)) {
      builder = builder.setScheduleDelayMillis(config.getLong(scheduleDelayMills))
    }

    builder.build()
  }

  abstract def createExporter(config: Config): SpanExporter
}
