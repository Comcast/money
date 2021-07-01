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

package com.comcast.money.core.handlers

import com.codahale.metrics.{ Histogram, Meter, MetricRegistry }
import com.comcast.money.core.CoreSpanInfo
import com.typesafe.config.Config
import io.opentelemetry.api.trace.StatusCode
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.OneInstancePerTest

class MetricsHandlerSpec extends AnyWordSpec with Matchers with MockitoSugar with TestData with OneInstancePerTest {

  val conf = mock[Config]
  when(conf.hasPath("metrics-registry.class-name")).thenReturn(true)
  when(conf.getString("metrics-registry.class-name")).thenReturn("com.comcast.money.core.metrics.MockMetricRegistryFactory")

  "MetricsSpanHandler" should {
    "configure the metrics registry" in {
      val underTest = MetricsSpanHandler(conf)

      underTest.metricRegistry shouldBe a[MetricRegistry]
    }

    "save latency metric" in {
      val underTest = MetricsSpanHandler(conf)

      val latencyMetric = mock[Histogram]
      val errorMetric = mock[Meter]
      when(underTest.metricRegistry.histogram(anyString())).thenReturn(latencyMetric)
      when(underTest.metricRegistry.meter(anyString())).thenReturn(errorMetric)

      underTest.handle(testSpanInfo)

      verify(latencyMetric).update(testSpanInfo.durationMicros)
      verifyNoMoreInteractions(errorMetric)
    }

    "update the error metric" in {
      val underTest = MetricsSpanHandler(conf)

      val latencyMetric = mock[Histogram]
      val errorMetric = mock[Meter]
      when(underTest.metricRegistry.histogram(anyString())).thenReturn(latencyMetric)
      when(underTest.metricRegistry.meter(anyString())).thenReturn(errorMetric)

      underTest.handle(testSpanInfo.toBuilder.status(StatusCode.ERROR).build())

      verify(latencyMetric).update(testSpanInfo.durationMicros)
      verify(errorMetric).mark()
    }
  }
}
