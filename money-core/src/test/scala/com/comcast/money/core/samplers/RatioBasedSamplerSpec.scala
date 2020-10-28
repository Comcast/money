/*
 * Copyright 2012 Comcast Cable Communications Management, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.comcast.money.core.samplers

import java.util.UUID

import com.comcast.money.api.{ IdGenerator, SpanId }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class RatioBasedSamplerSpec extends AnyWordSpec with Matchers {

  "RatioBasedSampler" should {
    "with a 0 or less ratio always return a drop result" in {
      val spanId = createSpanIdWithLoBits(0L)

      val underTest = new RatioBasedSampler(0.0)
      val result = underTest.shouldSample(spanId, None, "name")

      result shouldBe DropResult
    }

    "with a 1.0 or higher ratio always return a record result" in {
      val spanId = createSpanIdWithLoBits(Long.MaxValue - 1L)

      val underTest = new RatioBasedSampler(1.0)
      val result = underTest.shouldSample(spanId, None, "name")

      result should matchPattern { case RecordResult(true, _) => }
    }

    "records spans based on the low 64-bits of the trace id" in {
      val middleId = Long.MaxValue / 2L

      val underTest = new RatioBasedSampler(0.5)
      underTest.shouldSample(createSpanIdWithLoBits(middleId - 1L), None, "name") should matchPattern {
        case RecordResult(true, _) =>
      }

      underTest.shouldSample(createSpanIdWithLoBits(middleId + 1L), None, "name") should matchPattern {
        case DropResult =>
      }
    }

    "records the sampling probability as a note" in {
      val spanId = createSpanIdWithLoBits(0L)

      val underTest = new RatioBasedSampler(1.0)
      val result = underTest.shouldSample(spanId, None, "name")

      result should matchPattern {
        case RecordResult(true, List(note)) if note.name == "sampling.probability" && note.value == 1.0 =>
      }
    }
  }

  def createSpanIdWithLoBits(lo: Long): SpanId =
    SpanId.createFrom(new UUID(IdGenerator.generateRandomId(), lo), IdGenerator.generateRandomId(), IdGenerator.generateRandomId())
}
