package com.comcast.money.core.formatters

import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class FormatterFactorySpec extends AnyWordSpec with Matchers {
  "FormatterFactory" should {
    "create an instance of a Formatter" in {
      val config = ConfigFactory.parseString(s"class=${classOf[MoneyTraceFormatter].getCanonicalName}")

      val formatter = FormatterFactory.create(config)
      formatter shouldBe a [MoneyTraceFormatter]
    }
  }
}
