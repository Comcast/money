package com.comcast.money.core.samplers

import com.comcast.money.api.Note
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SamplerResultSpec extends AnyWordSpec with Matchers {
  "SamplerResult drop returns DropResult" in {
    val result = SamplerResult.drop()

    result shouldBe DropResult
  }

  "SamplerResult record returns RecordResult without sampling" in {
    val result = SamplerResult.record()

    result should matchPattern { case RecordResult(false, Nil) => }
  }

  "SamplerResult record with notes returns RecordResult without sampling" in {
    val note = Note.of("foo", "bar", false, 0L)
    val result = SamplerResult.record(Seq(note))

    result should matchPattern { case RecordResult(false, Seq(note)) => }
  }

  "SamplerResult recordAndSample returns RecordResult with sampling" in {
    val result = SamplerResult.recordAndSample()

    result should matchPattern { case RecordResult(true, Nil) => }
  }

  "SamplerResult recordAndSample with notes returns RecordResult with sampling" in {
    val note = Note.of("foo", "bar", false, 0L)
    val result = SamplerResult.recordAndSample(Seq(note))

    result should matchPattern { case RecordResult(true, Seq(note)) => }
  }
}
