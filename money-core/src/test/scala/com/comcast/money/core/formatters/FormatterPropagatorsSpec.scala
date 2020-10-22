package com.comcast.money.core.formatters

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

class FormatterPropagatorsSpec extends AnyWordSpec with MockitoSugar with Matchers {
  "FormatterPropagators" should {
    "wraps the Formatter in a FormatterProvider" in {
      val formatter = mock[Formatter]

      val underTest = FormatterPropagators(formatter)
      val propagator = underTest.getTextMapPropagator

      propagator shouldBe a[FormatterPropagator]
      val formatterPropagator = propagator.asInstanceOf[FormatterPropagator]
      formatterPropagator.formatter shouldBe formatter
    }
  }
}
