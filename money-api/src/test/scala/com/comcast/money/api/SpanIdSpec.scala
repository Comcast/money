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
    "create a new root span id" in {
      val spanId = SpanId.createNew()

      spanId.traceId should not be null
      spanId.selfId should not be 0
      spanId.isValid shouldBe true
      spanId.isRoot shouldBe true
      spanId.isRemote shouldBe false
      spanId.isSampled shouldBe true
    }

    "create a new non-sampled root span id" in {
      val spanId = SpanId.createNew(false)

      spanId.traceId should not be null
      spanId.selfId should not be 0
      spanId.isValid shouldBe true
      spanId.isRoot shouldBe true
      spanId.isRemote shouldBe false
      spanId.isSampled shouldBe false
    }

    "create a child span id" in {
      val parentId = SpanId.createNew(false)
      val childId = parentId.createChild()

      childId.traceId shouldBe parentId.traceId
      childId.parentId shouldBe parentId.selfId
      childId.isRoot shouldBe false
      childId.isValid shouldBe true
      childId.isRemote shouldBe false
      childId.isSampled shouldBe parentId.isSampled
    }

    "create a root span without a parent span id" in {
      val spanId = SpanId.createChild(null)

      spanId.isRoot shouldBe true
    }

    "create a root span with an invalid parent span id" in {
      val spanId = SpanId.createChild(SpanId.getInvalid)

      spanId.isRoot shouldBe true
    }

    "create a remote span id" in {
      val traceId = SpanId.randomTraceId()
      val selfId = SpanId.randomNonZeroLong()
      val parentId = SpanId.randomNonZeroLong()
      val state = TraceState.builder().set("foo", "bar").build()
      val remoteId = SpanId.createRemote(traceId, parentId, selfId, TraceFlags.getSampled, state)

      remoteId.traceId shouldBe traceId
      remoteId.parentId shouldBe parentId
      remoteId.selfId shouldBe selfId
      remoteId.isRoot shouldBe false
      remoteId.isValid shouldBe true
      remoteId.isRemote shouldBe true
      remoteId.isSampled shouldBe true
    }

    "fails to create a remote span with an invalid trace id" in {
      val traceId = "foo"
      val selfId = SpanId.randomNonZeroLong()
      val parentId = SpanId.randomNonZeroLong()

      assertThrows[IllegalArgumentException] {
        SpanId.createRemote(traceId, parentId, selfId, TraceFlags.getSampled, TraceState.getDefault)
      }
    }

    "create a child span id from a remote span id" in {
      val traceId = SpanId.randomTraceId()
      val selfId = SpanId.randomNonZeroLong()
      val parentId = SpanId.randomNonZeroLong()
      val state = TraceState.builder().set("foo", "bar").build()
      val remoteId = SpanId.createRemote(traceId, parentId, selfId, TraceFlags.getSampled, state)

      val childId = remoteId.createChild()

      childId.parentId shouldBe remoteId.selfId
      childId.isRoot shouldBe false
      childId.isValid shouldBe true
      childId.isRemote shouldBe false
      childId.isSampled shouldBe remoteId.isSampled
    }

    "creates span id from SpanContext" in {
      val spanContext = SpanContext.create("01234567890abcdef01234567890abcd", "0123456789abcdef", TraceFlags.getDefault, TraceState.getDefault)
      val spanId = SpanId.fromSpanContext(spanContext)

      spanId.traceId() shouldBe "01234567-890a-bcde-f012-34567890abcd"
      spanId.selfId() shouldBe 81985529216486895L
      spanId.parentId() shouldBe 81985529216486895L
    }

    "creates span id from invalid SpanContext" in {
      val spanContext = SpanContext.getInvalid
      val spanId = SpanId.fromSpanContext(spanContext)

      spanId.isValid shouldBe false
    }

    "parses a 128-bit hexadecimal string into a trace id" in {
      val hex = "01234567890abcdef01234567890abcd"

      val traceId = SpanId.parseTraceIdFromHex(hex)

      traceId shouldBe "01234567-890a-bcde-f012-34567890abcd"
    }

    "parses a 64-bit hexadecimal string into a trace id" in {
      val hex = "01234567890abcde"

      val traceId = SpanId.parseTraceIdFromHex(hex)

      traceId shouldBe "00000000-0000-0000-0123-4567890abcde"
    }

    "fails to parse a null hexadecimal string into a trace id" in {
      assertThrows[NullPointerException] {
        SpanId.parseTraceIdFromHex(null)
      }
    }

    "fails to parse garbage string into a trace id" in {
      assertThrows[IllegalArgumentException] {
        SpanId.parseTraceIdFromHex("foo")
      }
    }

    "parses a 64-bit hexadecimal string into a long id" in {
      val hex = "0123456789abcdef"

      val id = SpanId.parseIdFromHex(hex)

      id shouldBe 81985529216486895L
    }

    "fails to parse a null hexadecimal string into a long id" in {
      assertThrows[NullPointerException] {
        SpanId.parseIdFromHex(null)
      }
    }

    "fails to parse garbage hexadecimal string into a long id" in {
      assertThrows[IllegalArgumentException] {
        SpanId.parseIdFromHex("foo")
      }
    }

    "isValid returns false for an invalid span id" in {
      val invalidSpanId = SpanId.getInvalid
      invalidSpanId.isValid shouldBe false
    }

    "returns traceId as hex" in {
      val spanId = new SpanId("01234567-890A-BCDE-F012-34567890ABCD", 81985529216486895L, 81985529216486895L)

      spanId.traceIdAsHex shouldBe "01234567890abcdef01234567890abcd"
    }

    "returns traceId as is when not in expected format" in {
      val spanId = new SpanId("foo", 81985529216486895L, 81985529216486895L)

      spanId.traceIdAsHex shouldBe "foo"
    }

    "returns span id as hex" in {
      val spanId = new SpanId("01234567-890A-BCDE-F012-34567890ABCD", 81985529216486895L, 81985529216486895L)

      spanId.selfIdAsHex shouldBe "0123456789abcdef"
    }

    "returns parent span id as hex" in {
      val spanId = new SpanId("01234567-890A-BCDE-F012-34567890ABCD", 81985529216486895L, 81985529216486895L)

      spanId.parentIdAsHex shouldBe "0123456789abcdef"
    }

    "returns SpanContext from span id" in {
      val spanId = new SpanId("01234567-890A-BCDE-F012-34567890ABCD", 81985529216486895L, 81985529216486895L)
      val spanContext = spanId.toSpanContext

      spanContext.getTraceIdAsHexString shouldBe "01234567890abcdef01234567890abcd"
      spanContext.getSpanIdAsHexString shouldBe "0123456789abcdef"
      spanContext.getTraceFlags shouldBe TraceFlags.getSampled
      spanContext.getTraceState shouldBe TraceState.getDefault
    }

    "implements equality" in {
      val traceId = SpanId.randomTraceId()
      val selfId = SpanId.randomNonZeroLong()
      val spanId1 = new SpanId(traceId, selfId, selfId)
      val spanId2 = new SpanId(traceId, selfId, selfId)

      spanId1 shouldBe spanId2
      spanId1.hashCode() shouldBe spanId2.hashCode()

      val spanId3 = new SpanId(SpanId.randomTraceId(), selfId, selfId)

      spanId1 should not be spanId3
      spanId1.hashCode() should not be spanId3.hashCode()

      val otherId = SpanId.randomNonZeroLong()
      val spanId4 = new SpanId(traceId, otherId, otherId)

      spanId1 should not be spanId4
      spanId1.hashCode() should not be spanId4.hashCode()
    }

    "implements toString" in {
      val spanId = SpanId.createNew()

      val text = spanId.toString

      text should include(spanId.traceId)
      text should include(spanId.selfId.toString)
      text should include(spanId.parentId.toString)
    }
  }
}
