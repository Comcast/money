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

import java.util.UUID

import com.comcast.money.api.SpanId
import org.scalatest.{ Matchers, WordSpec }
import Formatters._
import org.scalacheck.Arbitrary
import org.scalacheck.Test.Failed
import org.scalatest.prop.GeneratorDrivenPropertyChecks

class FormattersSpec extends WordSpec with Matchers with GeneratorDrivenPropertyChecks with TraceGenerators {

  implicit val arbitraryUUID: Arbitrary[UUID] = Arbitrary(genUUID)

  "Http Formatting" should {
    "read a money http header" in {
      forAll { (traceIdValue: UUID, parentSpanIdValue: Long, spanIdValue: Long) =>
        val expectedSpanId = new SpanId(traceIdValue.toString, parentSpanIdValue, spanIdValue)
          val spanId = fromMoneyHeader(
            getHeader =  {
            case MoneyTraceHeader => MoneyHeaderFormat.format(expectedSpanId.traceId, expectedSpanId.parentId, expectedSpanId.selfId)
          }
          )
        spanId shouldBe Some(expectedSpanId)
      }
    }

    "fail to read a badly formatted money http header" in {
      forAll { (traceIdValue: String, parentSpanIdValue: String, spanIdValue: String) =>
        val spanId = fromMoneyHeader(
          getHeader =  {
          case MoneyTraceHeader => MoneyHeaderFormat.format(traceIdValue, parentSpanIdValue, spanIdValue)
        }
        )
        spanId shouldBe None
      }
    }

    "create a money http header" in {
      forAll { (traceIdValue: UUID, parentSpanIdValue: Long, spanIdValue: Long) =>
        val spanId = new SpanId(traceIdValue.toString, parentSpanIdValue, spanIdValue)
        toMoneyHeader(spanId, (header, value) => {
          header shouldBe Formatters.MoneyTraceHeader
          value shouldBe MoneyHeaderFormat.format(traceIdValue, parentSpanIdValue, spanIdValue)
        }
        )
      }
    }

    "read B3 headers correctly for any valid hex encoded headers: trace-Id , parent id and span ID" in {
      forAll { (traceIdValue: UUID, parentSpanIdValue: Long, spanIdValue: Long) =>
        val expectedSpanId = new SpanId(traceIdValue.toString, parentSpanIdValue, spanIdValue)
        val spanId = fromB3HttpHeaders(
          getHeader = {
          case B3TraceIdHeader => traceIdValue.toString.fromGuid
          case B3ParentSpanIdHeader => parentSpanIdValue.toHexString
          case B3SpanIdHeader => spanIdValue.toHexString
        }
        )
        spanId shouldBe Some(expectedSpanId)
        val maybeRootSpanId = fromB3HttpHeaders(
          getHeader = {
          case B3TraceIdHeader => traceIdValue.toString.fromGuid
          case B3SpanIdHeader => spanIdValue.toHexString
          case _ => null
        }
        )
        val rootSpanId = maybeRootSpanId
        rootSpanId should not be None
        rootSpanId.get.traceId shouldBe traceIdValue.toString
        rootSpanId.get.parentId shouldBe spanIdValue
        rootSpanId.get.selfId shouldBe spanIdValue
      }
    }

    "fail to read B3 headers correctly for invalid headers" in {
      forAll { (traceIdValue: String, parentSpanIdValue: String, spanIdValue: String) =>
        val spanId = fromB3HttpHeaders(
          getHeader = {
          case B3TraceIdHeader => traceIdValue
          case B3ParentSpanIdHeader => parentSpanIdValue
          case B3SpanIdHeader => spanIdValue
        }
        )
        spanId shouldBe None
      }
    }

    "create B3 headers correctly given any valid character UUID for trace-Id and any valid long integers for parent and span ID, where if parent == span id, parent will not be emitted" in {
      forAll { (traceIdValue: UUID, parentSpanIdValue: Long, spanIdValue: Long) =>
        val expectedSpanId = new SpanId(traceIdValue.toString, parentSpanIdValue, spanIdValue)
        Formatters.toB3Headers(expectedSpanId, (k, v) => k match {
          case B3TraceIdHeader if traceIdValue.getLeastSignificantBits == 0 => v shouldBe traceIdValue.toString.fromGuid.substring(0, 16)
          case B3TraceIdHeader => v shouldBe traceIdValue.toString.fromGuid
          case B3ParentSpanIdHeader if expectedSpanId.isRoot => Failed
          case B3ParentSpanIdHeader => parentSpanIdValue.toHexString
          case B3SpanIdHeader => v shouldBe spanIdValue.toHexString
        })
      }
    }

    "convert a string from hexadecimal to long" in {
      forAll(genHexStrFromLong) { hexStr: String =>
        {
          val actualLong = hexStr.fromHexStringToLong
          actualLong.toHexString shouldBe hexStr
        }
      }
    }

    "fail to convert a non-hex string from hexadecimal to long" in {
      intercept[NumberFormatException] { "".fromHexStringToLong }
      intercept[NumberFormatException] { "z".fromHexStringToLong }
    }

    "convert a string to guid format" in {
      forAll { str: String =>
        {
          val guid = str.toGuid
          guid.length shouldBe 36
          List(8, 13, 18, 23).foreach(ix => guid.charAt(ix) shouldBe '-')
        }
      }
    }

    "convert a string from guid format" in {
      forAll { uuid: UUID =>
        {
          val guid = uuid.toString
          val str = guid.fromGuid
          str.length shouldBe 32
          str.indexOf("-") shouldBe -1
        }
      }
    }
  }
}
