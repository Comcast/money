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

package com.comcast.money.core.internal

import java.util.ServiceLoader

import io.opentelemetry.context.ContextStorageProvider
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

import scala.collection.JavaConverters._

class CoreContextStorageProviderSpec extends AnyWordSpec with Matchers with MockitoSugar {
  "CoreContextStorageProviderSpec" should {
    "create a CoreContextStorage instance" in {
      val underTest = new CoreContextStorageProvider()
      val result = underTest.get()

      result shouldBe a[CoreContextStorage]
    }

    "integrates with Java SPI" in {
      val services = ServiceLoader.load(classOf[ContextStorageProvider])
      val result = services.asScala.headOption

      result.isDefined shouldBe true
      val provider = result.get
      provider shouldBe a[CoreContextStorageProvider]
    }
  }
}
