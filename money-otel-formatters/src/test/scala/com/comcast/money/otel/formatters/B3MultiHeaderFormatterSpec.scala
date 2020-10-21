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

package com.comcast.money.otel.formatters

import java.util.UUID

import com.comcast.money.api.SpanId
import com.comcast.money.core.TraceGenerators
import com.comcast.money.core.formatters.FormatterUtils.{LongToHexConversion, UUIDToHexConversion, isValidIds}
import com.comcast.money.otel.formatters.B3MultiHeaderFormatter.{B3ParentSpanIdHeader, B3SampledHeader, B3SpanIdHeader, B3TraceIdHeader}
import io.opentelemetry.trace.{TraceFlags, TraceState}
import org.mockito.Mockito.{verify, verifyNoMoreInteractions}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import scala.collection.mutable

class B3MultiHeaderFormatterSpec extends AnyWordSpec with MockitoSugar with Matchers with ScalaCheckDrivenPropertyChecks with TraceGenerators {
  val underTest = new B3MultiHeaderFormatter()
  val nullString = null.asInstanceOf[String]

  "B3MultiHeaderFormatter" should {
    "read B3 headers correctly for any valid hex encoded headers: trace-Id , parent id and span ID" in {
      forAll { (traceIdValue: UUID, spanIdValue: Long, sampled: Boolean) =>
        whenever(isValidIds(traceIdValue, spanIdValue)) {

          val expectedSpanId = SpanId.createRemote(traceIdValue.toString, spanIdValue, spanIdValue, TraceFlags.getSampled, TraceState.getDefault)
          val spanId = underTest.fromHttpHeaders(
            getHeader = {
              case B3TraceIdHeader => traceIdValue.hex64or128
              case B3SpanIdHeader => spanIdValue.hex64
              case B3SampledHeader => if (sampled) "1" else "0"
            })
          spanId shouldBe Some(expectedSpanId)
        }
      }
    }

    "fail to read B3 headers correctly for invalid headers" in {
      val spanId = underTest.fromHttpHeaders(getHeader = _ => "garbage")
      spanId shouldBe None
    }

    "create B3 headers correctly given any valid character UUID for trace-Id and any valid long integers for parent and span ID, where if parent == span id, parent will not be emitted" in {
      forAll { (traceIdValue: UUID, spanIdValue: Long, sampled: Boolean) =>
        whenever(isValidIds(traceIdValue, spanIdValue)) {

          val traceFlags = if (sampled) TraceFlags.getSampled else TraceFlags.getDefault
          val expectedSpanId = SpanId.createRemote(traceIdValue.toString, spanIdValue, spanIdValue, traceFlags, TraceState.getDefault)
          underTest.toHttpHeaders(expectedSpanId, (k, v) => k match {
            case B3TraceIdHeader => v shouldBe traceIdValue.hex64or128
            case B3SpanIdHeader => v shouldBe spanIdValue.hex64
            case B3SampledHeader if sampled => v shouldBe "1"
            case B3SampledHeader => v shouldBe "0"
          })
        }
      }
    }

    "can roundtrip a span id" in {
      val spanId = SpanId.createNew()

      val map = mutable.Map[String, String]()

      underTest.toHttpHeaders(spanId, map.put)

      val result = underTest.fromHttpHeaders(k => map.getOrElse(k, nullString))

      result shouldBe Some(spanId)
    }

    "lists the B3 headers" in {
      underTest.fields shouldBe Seq(B3TraceIdHeader, B3SpanIdHeader, B3ParentSpanIdHeader, B3SampledHeader)
    }

    "copy the request headers to the response" in {
      val setHeader = mock[(String, String) => Unit]

      underTest.setResponseHeaders({
        case B3TraceIdHeader => B3TraceIdHeader
        case B3SpanIdHeader => B3SpanIdHeader
        case B3ParentSpanIdHeader => B3ParentSpanIdHeader
        case B3SampledHeader => B3SampledHeader
      }, setHeader)

      verify(setHeader).apply(B3TraceIdHeader, B3TraceIdHeader)
      verify(setHeader).apply(B3SpanIdHeader, B3SpanIdHeader)
      verify(setHeader).apply(B3ParentSpanIdHeader, B3ParentSpanIdHeader)
      verify(setHeader).apply(B3SampledHeader, B3SampledHeader)
      verifyNoMoreInteractions(setHeader)
    }
  }
}
