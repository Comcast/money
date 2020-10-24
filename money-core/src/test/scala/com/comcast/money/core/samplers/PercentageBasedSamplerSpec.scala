package com.comcast.money.core.samplers

import java.util.UUID

import com.comcast.money.api.{IdGenerator, SpanId}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class PercentageBasedSamplerSpec extends AnyWordSpec with Matchers {

  "PercentageBasedSampler" should {
    "with a 0 or less percentage always return a drop result" in {
      val spanId = createSpanIdWithLoBits(0L)

      val underTest = new PercentageBasedSampler(0.0)
      val result = underTest.shouldSample(spanId, None, "name")

      result shouldBe DropResult
    }

    "with a 1.0 or higher percentage always return a record result" in {
      val spanId = createSpanIdWithLoBits(Long.MaxValue - 1L)

      val underTest = new PercentageBasedSampler(1.0)
      val result = underTest.shouldSample(spanId, None, "name")

      result should matchPattern { case RecordResult(true, _) => }
    }

    "records spans based on the low 64-bits of the trace id" in {
      val middleId = Long.MaxValue / 2L

      val underTest = new PercentageBasedSampler(0.5)
      underTest.shouldSample(createSpanIdWithLoBits(middleId - 1L), None, "name") should matchPattern {
        case RecordResult(true, _) =>
      }

      underTest.shouldSample(createSpanIdWithLoBits(middleId + 1L), None, "name") should matchPattern {
        case DropResult =>
      }
    }

    "records the sampler percentage as a note" in {
      val spanId = createSpanIdWithLoBits(0L)

      val underTest = new PercentageBasedSampler(1.0)
      val result = underTest.shouldSample(spanId, None, "name")

      result should matchPattern {
        case RecordResult(true, Seq(note)) if note.name == "sampling.probability" && note.value == 1.0 =>
      }
    }
  }

  def createSpanIdWithLoBits(lo: Long): SpanId =
    SpanId.createFrom(new UUID(IdGenerator.generateRandomId(), lo), IdGenerator.generateRandomId(), IdGenerator.generateRandomId())
}
