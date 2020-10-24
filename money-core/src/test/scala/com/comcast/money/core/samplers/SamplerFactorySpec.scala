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

    "return a PercentageBasedSampler" in {
      val config = ConfigFactory.parseString(
        """
          |type = "percentage-based"
          |percentage = 0.5
          |""".stripMargin)

      val sampler = SamplerFactory.create(config)
      sampler shouldBe a[PercentageBasedSampler]
      sampler.asInstanceOf[PercentageBasedSampler].percentage shouldBe 0.5
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
