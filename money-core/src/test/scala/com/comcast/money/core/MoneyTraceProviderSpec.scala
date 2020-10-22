package com.comcast.money.core

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

class MoneyTraceProviderSpec extends AnyWordSpec with MockitoSugar with Matchers {

  "MoneyTraceProvider" should {
    "wrap an existing tracer" in {
      val tracer = mock[Tracer]

      val underTest = MoneyTracerProvider(tracer)

      underTest.get("test") shouldBe tracer
      underTest.get("test", "1.0") shouldBe tracer
    }
  }
}
