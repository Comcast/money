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

package com.comcast.money.sampling

import com.comcast.money.core.{ Note, SpanId, Span }
import com.typesafe.config.ConfigFactory
import org.scalatest.{ Matchers, OneInstancePerTest, WordSpec }
import org.scalatest.mock.MockitoSugar

class SamplingSpec extends WordSpec with Matchers with MockitoSugar with OneInstancePerTest {

  val conf = ConfigFactory.parseString(
    """
      sampling {
        enabled = false
      }
    """
  )
  val sampleData = Span(
    SpanId(1L), "key", "unknown", "host", 1L, true, 35L,
    Map("what" -> Note("what", 1L), "when" -> Note("when", 2L), "bob" -> Note("bob", "craig"))
  )

  "Sampling" should {
    "return a no-op sampler if not configured" in {
      val conf = ConfigFactory.parseString(
        """
       money {
       }
        """
      )

      val sampler = Sampling.sampler(conf)

      for (i <- 1 to 1000) sampler.sample(sampleData) shouldBe true
    }
  }

  "EveryNSampler" should {
    "sample every other span" in {
      val conf = ConfigFactory.parseString(
        """
        sampling {
          enabled = true
          class-name = "com.comcast.money.sampling.EveryNSampler"
          every = 2
        }
        """
      )

      val sampler = Sampling.sampler(conf)

      sampler shouldBe a[EveryNSampler]

      var samples = 0
      for (i <- 1 to 1000) if (sampler.sample(sampleData)) samples += 1

      samples shouldBe 500
    }

    "sample every 10th span" in {
      val conf = ConfigFactory.parseString(
        """
        sampling {
          enabled = true
          class-name = "com.comcast.money.sampling.EveryNSampler"
          every = 10
        }
        """
      )

      val sampler = Sampling.sampler(conf)

      sampler shouldBe a[EveryNSampler]

      var samples = 0
      for (i <- 1 to 1000) if (sampler.sample(sampleData)) samples += 1

      samples shouldBe 100
    }
  }
}
