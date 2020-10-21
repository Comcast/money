package com.comcast.money.otel.formatters

import com.comcast.money.core.TraceGenerators
import com.comcast.money.otel.formatters.JaegerFormatter.UberTraceIdHeader
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class JaegerFormatterSpec extends AnyWordSpec with MockitoSugar with Matchers with ScalaCheckDrivenPropertyChecks with TraceGenerators {
  "JaegerFormatter" should {
    "lists the headers" in {
      JaegerFormatter.fields shouldBe Seq(UberTraceIdHeader)
    }
  }
}
