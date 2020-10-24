package com.comcast.money.core.samplers

import com.comcast.money.api.SpanId
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AlwaysOnSamplerSpec extends AnyWordSpec with Matchers {
  "AlwaysOnSampler" should {
    "always return a record result with sampling" in {
      val spanId = SpanId.createNew()
      AlwaysOnSampler.shouldSample(spanId, None, "name") should matchPattern { case RecordResult(true, Nil) => }
    }
  }
}
