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

import com.comcast.money.api.SpanId
import org.scalatest.{ Matchers, WordSpecLike }

class SpanIdSpec extends WordSpecLike with Matchers {

  "A SpanId" when {
    "created w/ an origin and parent" should {
      "creating an http header" should {
        "format correctly" in {
          val spanId = new SpanId("1", 2, 3)
          SpanId.toHttpHeader(spanId) shouldEqual "trace-id=1;parent-id=2;span-id=3"
        }
      }
      "parsing an http header" should {
        "produce the proper span id" in {
          val spanId = new SpanId("1", 2, 3)
          SpanId.fromHttpHeader(SpanId.toHttpHeader(spanId)) should be a 'success
        }
        "fail if the header is not formatted properly" in {
          SpanId.fromHttpHeader("foo") should be a 'failure
        }
      }
    }
  }
}
