package com.comcast.money.core.spi

import com.comcast.money.core.Tracer
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

class MoneyTracerProviderFactorySpec extends AnyWordSpec with MockitoSugar with Matchers {
  "MoneyTracerProviderFactory" should {
    "wrap an existing tracer in a TracerProvider" in {
      val tracer = mock[Tracer]
      val underTest = new MoneyTracerProviderFactory(tracer)

      val provider = underTest.create()
      provider.get("test") shouldBe tracer
      provider.get("test", "1.0") shouldBe tracer
    }
  }
}
