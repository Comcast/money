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

package com.comcast.money.metrics

import akka.actor.{ Actor, ActorLogging, Props }
import com.codahale.metrics._
import com.comcast.money.akka.ActorMaker
import com.comcast.money.core.Span
import com.typesafe.config.Config

object SpanMetrics {

  def props(spanName: String, registry: MetricRegistry) = {
    val latencyMetric: Histogram = registry.histogram(s"/money/$spanName:latency")
    val errorMetric: Meter = registry.meter(s"/money/$spanName:error")
    Props(classOf[SpanMetrics], spanName, latencyMetric, errorMetric)
  }
}

class SpanMetrics(spanName: String, latencyMetric: Histogram, errorMetric: Meter) extends Actor with ActorLogging {

  def receive = {
    case span: Span =>
      if (!span.success)
        errorMetric.mark()

      latencyMetric.update(span.duration)
  }
}

class SpanMetricsCollector(conf: Config) extends Actor with ActorMaker with ActorLogging {

  import com.comcast.money.internal.EmitterProtocol._

  def receive = {
    case EmitSpan(t: Span) =>
      context.child(t.spanName) match {
        case Some(spanMetrics) =>
          spanMetrics forward t
        case None =>
          val metricRegistry = MetricRegistryFactory.metricRegistry(conf)
          val escapedName = t.spanName.replace(' ', '.')
          log.debug(s"Creating span metrics for span $escapedName")
          makeActor(SpanMetrics.props(escapedName, metricRegistry), escapedName) forward t
      }
  }
}
