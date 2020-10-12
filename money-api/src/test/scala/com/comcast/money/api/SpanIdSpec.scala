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

package com.comcast.money.api

import io.opentelemetry.trace.{ SpanContext, TraceFlags, TraceState }
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

class SpanIdSpec extends AnyWordSpec with Matchers {

  "SpanId" should {
    "take 3 constructor arguments" in {
      val spanId = new SpanId("foo", 1L, 2L)

      spanId.traceId shouldBe "foo"
      spanId.parentId shouldBe 1L
      spanId.selfId shouldBe 2L
    }

    "set self id to a random long if not specified in the constructor" in {
      val spanId = new SpanId("foo", 1L)

      spanId.traceId shouldBe "foo"
      spanId.parentId shouldBe 1L
      Long.box(spanId.selfId) should not be null
    }

    "set the self and parent id to a random long if not specified" in {
      val spanId = new SpanId("foo")

      spanId.traceId shouldBe "foo"
      Long.box(spanId.parentId) should not be null
      Long.box(spanId.selfId) should not be null
    }

    "set the self id to the parent id when neither is specified" in {
      val spanId: SpanId = new SpanId()
      assert(spanId.parentId === spanId.selfId)
    }

    "generate a string matching SpanId~%s~%s~%s" in {
      val format = "SpanId~%s~%s~%s"
      val expected = format.format("foo", 1L, 2L)

      val spanId = new SpanId("foo", 1L, 2L)
      val result = spanId.toString

      result shouldEqual expected
    }

    "parse a string into a span id" in {
      val spanId = new SpanId("foo", 1L, 2L)
      val str = spanId.toString

      val parsed = SpanId.fromString(str)
      parsed.traceId shouldBe spanId.traceId
      parsed.parentId shouldBe spanId.parentId
      parsed.selfId shouldBe spanId.selfId
    }

    "default traceId to UUID if set to null" in {
      val spanId = new SpanId(null, 1L)

      spanId.traceId should not be null
    }

    "isRoot returns true for a root span id" in {
      val spanId = new SpanId("foo", 1L, 1L)
      spanId.isRoot shouldBe true

      val childSpanId = spanId.newChildId()
      childSpanId.isRoot shouldBe false
    }

    "isValid returns false for an invalid span id" in {
      val invalidSpanId = new SpanId("", 0L, 0L)
      invalidSpanId.isValid shouldBe false

      SpanId.getInvalid.isValid shouldBe false
    }

    "returns SpanContext from span id" in {
      val spanId = new SpanId("01234567-890A-BCDE-F012-34567890ABCD", 81985529216486895L, 81985529216486895L)
      val spanContext = spanId.toSpanContext

      spanContext.getTraceIdAsHexString shouldBe "01234567890abcdef01234567890abcd"
      spanContext.getSpanIdAsHexString shouldBe "0123456789abcdef"
      spanContext.getTraceFlags shouldBe TraceFlags.getDefault
      spanContext.getTraceState shouldBe TraceState.getDefault
    }

    "returns span if from SpanContext" in {
      val spanContext = SpanContext.create("01234567890abcdef01234567890abcd", "0123456789abcdef", TraceFlags.getDefault, TraceState.getDefault)
      val spanId = SpanId.fromSpanContext(spanContext)

      spanId.traceId() shouldBe "01234567-890a-bcde-f012-34567890abcd"
      spanId.selfId() shouldBe 81985529216486895L
      spanId.parentId() shouldBe 81985529216486895L
    }
  }
}
