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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class IdGeneratorSpec extends AnyWordSpec with Matchers {

  "generates a random trace id" in {
    val traceId = IdGenerator.generateRandomTraceId()

    traceId should have size 36
    traceId should fullyMatch regex """^([0-9a-f]{8})-?([0-9a-f]{4})-([0-9a-f]{4})-?([0-9a-f]{4})-?([0-9a-f]{12})$"""
    traceId should not be IdGenerator.INVALID_TRACE_ID
  }

  "generates a random id" in {
    val id = IdGenerator.generateRandomId()

    id should not be 0
  }

  "generates a random 128-bit hex trace id" in {
    val traceId = IdGenerator.generateRandomTraceIdAsHex()

    traceId should have size 32
    traceId should fullyMatch regex """^[0-9a-f]{32}$"""
    traceId should not be IdGenerator.INVALID_TRACE_ID_HEX
  }

  "generates a random 64-bit hex id" in {
    val id = IdGenerator.generateRandomIdAsHex()

    id should have size 16
    id should fullyMatch regex """^[0-9a-f]{16}$"""
    id should not be IdGenerator.INVALID_ID_HEX
  }

  "converts a trace id to hex" in {
    val traceId = "01234567-890A-BCDE-F012-34567890ABCD"

    val hex = IdGenerator.convertTraceIdToHex(traceId)

    hex shouldBe "01234567890abcdef01234567890abcd"
  }

  "converts an invalid trace id to hex" in {
    val traceId = "foo"

    val hex = IdGenerator.convertTraceIdToHex(traceId)

    hex shouldBe IdGenerator.INVALID_TRACE_ID_HEX
  }

  "converts an id to hex" in {
    val id = 81985529216486895L

    val hex = IdGenerator.convertIdToHex(id)

    hex shouldBe "0123456789abcdef"
  }

  "detects valid trace id" in {
    val traceId = "01234567-890A-BCDE-F012-34567890ABCD"
    IdGenerator.isValidTraceId(traceId) shouldBe true
  }

  "detects invalid trace id" in {
    val traceId = "foo"
    IdGenerator.isValidTraceId(traceId) shouldBe false
  }

  "detects valid hex trace id" in {
    val traceId = "01234567890abcdef01234567890abcd"
    IdGenerator.isValidHexTraceId(traceId) shouldBe true
  }

  "detects invalid hex trace id" in {
    val traceId = "01234567-890A-BCDE-F012-34567890ABCD"
    IdGenerator.isValidHexTraceId(traceId) shouldBe false
  }

  "parse 128-bit hex to trace id" in {
    val hex = "01234567890abcdef01234567890abcd"
    val traceId = IdGenerator.parseTraceIdFromHex(hex)

    traceId shouldBe "01234567-890a-bcde-f012-34567890abcd"
  }

  "parse 64-bit hex to trace id" in {
    val hex = "001234567890abcd"
    val traceId = IdGenerator.parseTraceIdFromHex(hex)

    traceId shouldBe "00000000-0000-0000-0012-34567890abcd"
  }

  "parse invalid hex trace id" in {
    val traceId = IdGenerator.parseTraceIdFromHex("foo")

    traceId shouldBe IdGenerator.INVALID_TRACE_ID
  }

  "parse 64-bit hex to id" in {
    val id = IdGenerator.parseIdFromHex("0123456789abcdef")

    id shouldBe 81985529216486895L
  }

  "parse invalid hex id" in {
    val id = IdGenerator.parseIdFromHex("foo")

    id shouldBe IdGenerator.INVALID_ID
  }
}
