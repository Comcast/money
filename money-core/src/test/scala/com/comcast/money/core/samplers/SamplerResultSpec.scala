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
