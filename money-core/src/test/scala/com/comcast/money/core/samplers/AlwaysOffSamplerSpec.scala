package com.comcast.money.core.samplers

import com.comcast.money.api.SpanId
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AlwaysOffSamplerSpec extends AnyWordSpec with Matchers {
  "AlwaysOffSampler" should {
    "always return a drop result" in {
      val spanId = SpanId.createNew()
      AlwaysOffSampler.shouldSample(spanId, None, "name") shouldBe DropResult
    }
  }
}
