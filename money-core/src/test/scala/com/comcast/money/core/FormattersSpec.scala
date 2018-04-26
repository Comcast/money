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
import org.scalatest.{ Matchers, WordSpec }

class FormattersSpec extends WordSpec with Matchers {

  "Http Formatting" should {
    "convert from a money  http header" in {
      val spanId = new SpanId()
      val test = Formatters.toHttpHeader(spanId)

      Formatters.fromHttpHeader(test).get shouldBe spanId
    }

    "convert from all X-B3  http headers" in {
      val expectedSpanId = new SpanId("a"*32,Long.MaxValue,Long.MinValue)

      val actualSpanId = Formatters.fromB3HttpHeaders(expectedSpanId.traceId, Option(expectedSpanId.parentId().toString),Option(expectedSpanId.selfId().toString)).get
      actualSpanId shouldBe expectedSpanId
    }


    "convert from 2 X-B3  http headers" in {

      val actualSpanId = Formatters.fromB3HttpHeaders("a", Option("1"), None).get
      actualSpanId.traceId shouldBe "a"
      actualSpanId.parentId shouldBe 1L
    }

    "convert from 1 X-B3  http header" in {
      val spanId = new SpanId("a",1,2)

      val actualSpanId = Formatters.fromB3HttpHeaders("a",None, None).get
      actualSpanId.traceId shouldBe "a"
    }
  }


}
