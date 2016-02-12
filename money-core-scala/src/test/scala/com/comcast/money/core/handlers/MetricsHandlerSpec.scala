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

import com.codahale.metrics.{ Meter, Histogram, MetricRegistry }
import com.typesafe.config.Config
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ OneInstancePerTest, Matchers, WordSpec }

class MetricsHandlerSpec extends WordSpec with Matchers with MockitoSugar with TestData with OneInstancePerTest {

  val conf = mock[Config]
  doReturn(true).when(conf).hasPath("metrics-registry.class-name")
  doReturn("com.comcast.money.core.metrics.MockMetricRegistryFactory").when(conf).getString("metrics-registry.class-name")

  "MetricsSpanHandler" should {
    "configure the metrics registry" in {
      val underTest = new MetricsSpanHandler()
      underTest.configure(conf)

      underTest.metricRegistry shouldBe a[MetricRegistry]
    }

    "save latency metric" in {
      val underTest = new MetricsSpanHandler()
      underTest.configure(conf)

      val latencyMetric = mock[Histogram]
      val errorMetric = mock[Meter]
      doReturn(latencyMetric).when(underTest.metricRegistry).histogram(anyString())
      doReturn(errorMetric).when(underTest.metricRegistry).meter(anyString())

      underTest.handle(testSpanInfo)

      verify(latencyMetric).update(testSpanInfo.durationMicros)
      verifyZeroInteractions(errorMetric)
    }

    "update the error metric" in {
      val underTest = new MetricsSpanHandler()
      underTest.configure(conf)

      val latencyMetric = mock[Histogram]
      val errorMetric = mock[Meter]
      doReturn(latencyMetric).when(underTest.metricRegistry).histogram(anyString())
      doReturn(errorMetric).when(underTest.metricRegistry).meter(anyString())

      underTest.handle(testSpanInfo.copy(success = false))

      verify(latencyMetric).update(testSpanInfo.durationMicros)
      verify(errorMetric).mark()
    }
  }
}
