/*
 * Copyright 2012-2015 Comcast Cable Communications Management, LLC
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

package com.comcast.money.core

import org.scalatest.{Matchers, WordSpecLike}

class SpanIdSpec extends WordSpecLike with Matchers {

  "A SpanId" when {
    "created w/ no parameters" should {
      "generate a spanId and use it for parentId" in {
        val spanId: SpanId = SpanId()
        assert(spanId.parentSpanId === spanId.spanId)
      }
    }
    "created w/ an origin and parent" should {
      "generate a spanId" in {
        val spanId: SpanId = SpanId("1", 2L)
        assert(spanId.traceId === "1")
        assert(spanId.parentSpanId === 2L)
      }
      "created" should {
        "an akka friendly toString value" in {
          val spanId: SpanId = SpanId("1", 2L)
          assert(spanId.toString startsWith "SpanId~1~2")
        }
      }
      "created from a string" should {
        "contain the parts that were provided in the string" in {
          val spanIdAttempt = SpanId("SpanId~1~2~3")
          spanIdAttempt.isSuccess shouldBe true
          spanIdAttempt.get shouldEqual SpanId("1", 2, 3)
        }
        "return a failure if the string could not be parsed" in {
          val spanIdAttempt = SpanId("can't parse this, dooo do do do, do do, do do")
          spanIdAttempt.isFailure shouldBe true
        }
      }
      "creating an http header" should {
        "format correctly" in {
          val spanId = SpanId("1", 2, 3)
          spanId.toHttpHeader shouldEqual "trace-id=1;parent-id=2;span-id=3"
        }
      }
      "parsing an http header" should {
        "produce the proper span id" in {
          val spanId = SpanId("1", 2, 3)
          SpanId.fromHttpHeader(spanId.toHttpHeader) should be a 'success
        }
        "fail if the header is not formatted properly" in {
          SpanId.fromHttpHeader("foo") should be a 'failure
        }
      }
    }
  }
}
