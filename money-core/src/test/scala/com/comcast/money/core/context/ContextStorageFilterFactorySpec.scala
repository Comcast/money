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

package com.comcast.money.core.context

import com.comcast.money.core.FactoryException
import com.typesafe.config.ConfigFactory
import org.scalatest.Inside.inside
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

import scala.util.{Failure, Success}

class ContextStorageFilterFactorySpec extends AnyWordSpec with Matchers with MockitoSugar {

  "ContextStorageFilterFactory" should {
    "return an instance of mdc filter" in {
      val config = ConfigFactory.parseString("type = \"mdc\"")

      val filter = ContextStorageFilterFactory.create(config)
      inside(filter) {
        case Success(_: FormattedMdcContextStorageFilter) =>
      }
    }

    "return an instance of structured-mdc filter" in {
      val config = ConfigFactory.parseString("type = \"structured-mdc\"")

      val filter = ContextStorageFilterFactory.create(config)
      inside(filter) {
        case Success(_: StructuredMdcContextStorageFilter) =>
      }
    }

    "create an instance of a custom ContextStorageFilter" in {
      val config = ConfigFactory.parseString(
        s"""
           |class = "${classOf[NonConfiguredContextStorageFilter].getCanonicalName}"
           |""".stripMargin)

      val filter = ContextStorageFilterFactory.create(config)
      inside(filter) {
        case Success(_: NonConfiguredContextStorageFilter) =>
      }
    }

    "create an instance of a custom configurable ContextStorageFilter" in {
      val config = ConfigFactory.parseString(
        s"""
           |class = "${classOf[ConfiguredContextStorageFilter].getCanonicalName}"
           |""".stripMargin)

      val filter = ContextStorageFilterFactory.create(config)
      inside(filter) {
        case Success(f: ConfiguredContextStorageFilter) =>
          f.config shouldBe config
      }
    }

    "returns a disabled filter on an unknown type" in {
      val config = ConfigFactory.parseString("type = \"unknown\"")

      val filter = ContextStorageFilterFactory.create(config)
      inside (filter) {
        case Failure(exception: FactoryException) =>
          exception.getMessage shouldBe "Could not resolve known ContextStorageFilter type 'unknown'."
      }
    }
  }
}
