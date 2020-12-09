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

package com.comcast.money.core.spi

import java.util.ServiceLoader

import com.comcast.money.core.{ MoneyTracerProvider, Tracer }
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.spi.trace.TracerProviderFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

import scala.collection.JavaConverters._

class MoneyTracerProviderFactorySpec extends AnyWordSpec with MockitoSugar with Matchers {

  "MoneyTracerProviderFactory" should {
    "wrap an existing tracer in a TracerProvider" in {
      val tracer = mock[Tracer]
      val underTest = new MoneyTracerProviderFactory(tracer)

      val provider = underTest.create()
      provider shouldBe a[MoneyTracerProvider]
    }

    "integrates with Java SPI" in {
      val services = ServiceLoader.load(classOf[TracerProviderFactory])
      val result = services.asScala.headOption

      result.isDefined shouldBe true
      val factory = result.get
      factory shouldBe a[MoneyTracerProviderFactory]
    }

    "integrates with OpenTelemetry.getTracer" in {
      val tracer = OpenTelemetry.getGlobalTracer("test")
      tracer shouldBe a[Tracer]
    }
  }
}
