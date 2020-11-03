package com.comcast.money.core.internal

import java.util.ServiceLoader

import io.opentelemetry.context.ContextStorageProvider
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

import scala.collection.JavaConverters._

class CoreContextStorageProviderSpec extends AnyWordSpec with Matchers with MockitoSugar {
  "CoreContextStorageProviderSpec" should {
    "create a CoreContextStorage instance" in {
      val underTest = new CoreContextStorageProvider()
      val result = underTest.get()

      result shouldBe a[CoreContextStorage]
    }

    "integrates with Java SPI" in {
      val services = ServiceLoader.load(classOf[ContextStorageProvider])
      val result = services.asScala.headOption

      result.isDefined shouldBe true
      val provider = result.get
      provider shouldBe a[CoreContextStorageProvider]
    }
  }
}
