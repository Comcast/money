package com.comcast.money.core.spi

import java.util.ServiceLoader

import com.comcast.money.core.Tracer
import io.opentelemetry.trace.spi.TracerProviderFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import scala.collection.JavaConverters._

class MoneyTracerProviderFactorySpec extends AnyWordSpec with MockitoSugar with Matchers {
  "MoneyTracerProviderFactory" should {
    "wrap an existing tracer in a TracerProvider" in {
      val tracer = mock[Tracer]
      val underTest = new MoneyTracerProviderFactory(tracer)

      val provider = underTest.create()
      provider.get("test") shouldBe tracer
      provider.get("test", "1.0") shouldBe tracer
    }

    "integrates with Java SPI" in {
      val services = ServiceLoader.load(classOf[TracerProviderFactory])
      val result = services.asScala.headOption

      result.isDefined shouldBe true
      val factory = result.get
      factory shouldBe a[MoneyTracerProviderFactory]
    }
  }
}
