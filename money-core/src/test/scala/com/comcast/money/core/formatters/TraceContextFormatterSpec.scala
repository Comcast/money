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

package com.comcast.money.core.formatters

import java.util.UUID

import com.comcast.money.api.SpanId
import com.comcast.money.core.formatters.TraceContextFormatter.{ TraceParentHeader, TraceStateHeader }
import com.comcast.money.core.TraceGenerators
import io.opentelemetry.api.trace.{ TraceFlags, TraceState }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import FormatterUtils._
import org.mockito.Mockito.{ verify, verifyNoMoreInteractions }
import org.scalatestplus.mockito.MockitoSugar

class TraceContextFormatterSpec extends AnyWordSpec with MockitoSugar with Matchers with ScalaCheckDrivenPropertyChecks with TraceGenerators {
  val underTest: Formatter = TraceContextFormatter

  "TraceContextFormatter" should {
    "read a traceparent http header" in {
      forAll { (traceIdValue: UUID, spanIdValue: Long, sampled: Boolean) =>
        whenever(isValidIds(traceIdValue, spanIdValue)) {

          val spanId = underTest.fromHttpHeaders(
            Seq(),
            getHeader = {
              case TraceParentHeader => s"00-${traceIdValue.hex128}-${spanIdValue.hex64}-${if (sampled) "01" else "00"}"
              case TraceStateHeader => "foo=bar"
            })
          spanId should not be None
          spanId.get.traceId shouldBe traceIdValue.id
          spanId.get.selfId shouldBe spanIdValue
          spanId.get.isRemote shouldBe true
          spanId.get.isSampled shouldBe sampled
        }
      }
    }

    "fail to read traceparent headers correctly for invalid headers" in {
      val spanId = underTest.fromHttpHeaders(Seq(), _ => "garbage")
      spanId shouldBe None
    }

    "create traceparent headers correctly given any valid character UUID for trace-Id and any valid long integers for parent and span ID" in {
      forAll { (traceIdValue: UUID, spanIdValue: Long, sampled: Boolean) =>
        whenever(isValidIds(traceIdValue, spanIdValue)) {
          val traceFlags = if (sampled) TraceFlags.getSampled else TraceFlags.getDefault
          val traceState = TraceState.builder().set("foo", "bar").build()
          val expectedSpanId = SpanId.createRemote(traceIdValue.toString, spanIdValue, spanIdValue, traceFlags, traceState)

          underTest.toHttpHeaders(expectedSpanId, (k, v) => k match {
            case TraceParentHeader => v shouldBe s"00-${traceIdValue.hex128}-${spanIdValue.hex64}-${if (sampled) "01" else "00"}"
            case TraceStateHeader => v shouldBe "foo=bar"
          })
        }
      }
    }

    "lists the Trace Context headers" in {
      underTest.fields shouldBe Seq(TraceParentHeader, TraceStateHeader)
    }

    "copy the request headers to the response" in {
      val setHeader = mock[(String, String) => Unit]

      underTest.setResponseHeaders({
        case TraceParentHeader => TraceParentHeader
        case TraceStateHeader => TraceStateHeader
      }, setHeader)

      verify(setHeader).apply(TraceParentHeader, TraceParentHeader)
      verify(setHeader).apply(TraceStateHeader, TraceStateHeader)
      verifyNoMoreInteractions(setHeader)
    }
  }
}
