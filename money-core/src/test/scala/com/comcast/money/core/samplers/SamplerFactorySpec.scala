package com.comcast.money.core.samplers

import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SamplerFactorySpec extends AnyWordSpec with Matchers {
  "SamplerFactory" should {
    "return AlwaysOnSampler" in {
      val config = ConfigFactory.parseString("""type = "always-on"""")

      val sampler = SamplerFactory.create(config)
      sampler shouldBe AlwaysOnSampler
    }

    "return AlwaysOffSampler" in {
      val config = ConfigFactory.parseString("""type = "always-off"""")

      val sampler = SamplerFactory.create(config)
      sampler shouldBe AlwaysOffSampler
    }

    "return a RatioBasedSampler" in {
      val config = ConfigFactory.parseString(
        """
          |type = "ratio-based"
          |ratio = 0.5
          |""".stripMargin)

      val sampler = SamplerFactory.create(config)
      sampler shouldBe a[RatioBasedSampler]
      sampler.asInstanceOf[RatioBasedSampler].ratio shouldBe 0.5
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
      sampler shouldBe a[ParentBasedSampler]

      val parentBasedSampler = sampler.asInstanceOf[ParentBasedSampler]
      parentBasedSampler.root shouldBe AlwaysOffSampler
      parentBasedSampler.localSampled shouldBe AlwaysOffSampler
      parentBasedSampler.localNotSampled shouldBe AlwaysOnSampler
      parentBasedSampler.remoteSampled shouldBe AlwaysOffSampler
      parentBasedSampler.remoteNotSampled shouldBe AlwaysOnSampler
    }

    "create a custom sampler by class name" in {
      val samplerClassName = classOf[TestSampler].getCanonicalName
      val config = ConfigFactory.parseString(
        s"""
           |type = "custom"
           |class = "$samplerClassName"
           |""".stripMargin)

      val sampler = SamplerFactory.create(config)
      sampler shouldBe a[TestSampler]
    }

    "create a custom configurable sampler by class name" in {
      val samplerClassName = classOf[TestConfigurableSampler].getCanonicalName
      val config = ConfigFactory.parseString(
        s"""
           |type = "custom"
           |class = "$samplerClassName"
           |""".stripMargin)

      val sampler = SamplerFactory.create(config)
      sampler shouldBe a[TestConfigurableSampler]
      sampler.asInstanceOf[TestConfigurableSampler].calledConfigured shouldBe true
    }
  }
}
