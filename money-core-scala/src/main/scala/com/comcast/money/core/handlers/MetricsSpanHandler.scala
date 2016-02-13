/*
 * Copyright 2012-2015 Comcast Cable Communications Management, LLC
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

package com.comcast.money.core.handlers

import com.codahale.metrics.{ Histogram, Meter, MetricRegistry }
import com.comcast.money.api.SpanInfo
import com.comcast.money.core.metrics.MetricRegistryFactory
import com.typesafe.config.Config

import scala.collection.concurrent.TrieMap

case class SpanMetrics(latencyMetric: Histogram, errorMetric: Meter) {

  def record(spanInfo: SpanInfo): Unit = {
    if (!spanInfo.success)
      errorMetric.mark()

    latencyMetric.update(spanInfo.durationMicros)
  }
}

class MetricsSpanHandler extends ConfigurableHandler {

  private[handlers] var metricRegistry: MetricRegistry = _

  private[handlers] val spans = new TrieMap[String, SpanMetrics]()

  def configure(config: Config): Unit = {

    metricRegistry = MetricRegistryFactory.metricRegistry(config)
  }

  def handle(span: SpanInfo): Unit =
    spans.getOrElseUpdate(span.name, spanMetrics(span.name)).record(span)

  private def spanMetrics(spanName: String): SpanMetrics = {
    val latencyMetric: Histogram = metricRegistry.histogram(s"/money/$spanName:latency")
    val errorMetric: Meter = metricRegistry.meter(s"/money/$spanName:error")
    SpanMetrics(latencyMetric, errorMetric)
  }
}
