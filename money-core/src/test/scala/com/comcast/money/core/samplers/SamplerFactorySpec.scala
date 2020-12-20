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

package com.comcast.money.core.samplers

import com.typesafe.config.ConfigFactory
import org.scalatest.Inside.inside
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SamplerFactorySpec extends AnyWordSpec with Matchers {
  "SamplerFactory" should {
    "return AlwaysOnSampler" in {
      val config = ConfigFactory.parseString("""type = "always-on"""")

      val sampler = SamplerFactory.create(config)
      sampler shouldBe Some(AlwaysOnSampler)
    }

    "return AlwaysOffSampler" in {
      val config = ConfigFactory.parseString("""type = "always-off"""")

      val sampler = SamplerFactory.create(config)
      sampler shouldBe Some(AlwaysOffSampler)
    }

    "return a RatioBasedSampler" in {
      val config = ConfigFactory.parseString(
        """
          |type = "ratio-based"
          |ratio = 0.5
          |""".stripMargin)

      val sampler = SamplerFactory.create(config)
      inside(sampler) {
        case Some(ratioBasedSampler: RatioBasedSampler) =>
          ratioBasedSampler.ratio shouldBe 0.5
      }
    }

    "return a ParentBasedSampler" in {
      val config = ConfigFactory.parseString(
        """
          |type = "parent-based"
          |root = { type = "always-off" }
          |local-sampled = { type = "always-off" }
          |local-not-sampled = { type = "always-on" }
          |remote-sampled = { type = "always-off" }
          |remote-not-sampled = { type = "always-on" }
          |""".stripMargin)

      val sampler = SamplerFactory.create(config)
      inside(sampler) {
        case Some(parentBasedSampler: ParentBasedSampler) =>
          parentBasedSampler.root shouldBe AlwaysOffSampler
          parentBasedSampler.localSampled shouldBe AlwaysOffSampler
          parentBasedSampler.localNotSampled shouldBe AlwaysOnSampler
          parentBasedSampler.remoteSampled shouldBe AlwaysOffSampler
          parentBasedSampler.remoteNotSampled shouldBe AlwaysOnSampler
      }
    }

    "create a custom sampler by class name" in {
      val samplerClassName = classOf[TestSampler].getCanonicalName
      val config = ConfigFactory.parseString(
        s"""
           |class = "$samplerClassName"
           |""".stripMargin)

      val sampler = SamplerFactory.create(config)
      inside(sampler) {
        case Some(_: TestSampler) =>
      }
    }

    "create a custom configurable sampler by class name" in {
      val samplerClassName = classOf[TestConfigurableSampler].getCanonicalName
      val config = ConfigFactory.parseString(
        s"""
           |class = "$samplerClassName"
           |""".stripMargin)

      val sampler = SamplerFactory.create(config)
      inside(sampler) {
        case Some(s: TestConfigurableSampler) =>
          s.config shouldBe config
      }
    }
  }
}
