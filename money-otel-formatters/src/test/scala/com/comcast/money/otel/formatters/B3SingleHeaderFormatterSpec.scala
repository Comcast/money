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
import com.comcast.money.core.formatters.FormatterUtils._
import com.comcast.money.otel.formatters.B3SingleHeaderFormatter.B3Header
import io.opentelemetry.trace.{TraceFlags, TraceState}
import org.mockito.Mockito.{verify, verifyNoMoreInteractions}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class B3SingleHeaderFormatterSpec extends AnyWordSpec with MockitoSugar with Matchers with ScalaCheckDrivenPropertyChecks with TraceGenerators {
  "B3SingleHeaderFormatter" should {
    "read B3 header correctly for any valid hex encoded header" in {
      forAll { (traceIdValue: UUID, spanIdValue: Long, sampled: Boolean, debug: Boolean) =>
        whenever(isValidIds(traceIdValue, spanIdValue)) {
          val traceFlags = if (sampled | debug) TraceFlags.getSampled else TraceFlags.getDefault
          val expectedSpanId = SpanId.createRemote(traceIdValue.toString, spanIdValue, spanIdValue, traceFlags, TraceState.getDefault)
          val spanId = B3SingleHeaderFormatter.fromHttpHeaders(
            getHeader = {
              case B3Header => f"${traceIdValue.hex64or128}-${spanIdValue.hex64}-${sampledFlag(sampled, debug)}"
            })
          spanId shouldBe Some(expectedSpanId)
        }
      }
    }

    "fail to read B3 headers correctly for invalid headers" in {
      val spanId = B3SingleHeaderFormatter.fromHttpHeaders(getHeader = _ => "garbage")
      spanId shouldBe None
    }

    "create B3 header correctly given any valid character UUID for trace-Id and any valid long integers for parent and span ID, where if parent == span id, parent will not be emitted" in {
      forAll { (traceIdValue: UUID, spanIdValue: Long, sampled: Boolean) =>
        whenever(isValidIds(traceIdValue, spanIdValue)) {

          val traceFlags = if (sampled) TraceFlags.getSampled else TraceFlags.getDefault
          val expectedSpanId = SpanId.createRemote(traceIdValue.toString, spanIdValue, spanIdValue, traceFlags, TraceState.getDefault)
          B3SingleHeaderFormatter.toHttpHeaders(expectedSpanId, (k, v) => k match {
            case B3Header => v shouldBe f"${traceIdValue.hex128}-${spanIdValue.hex64}-${if (sampled) "1" else "0"}"
          })
        }
      }
    }

    "lists the B3 headers" in {
      B3SingleHeaderFormatter.fields shouldBe Seq(B3Header)
    }

    "copy the request headers to the response" in {
      val setHeader = mock[(String, String) => Unit]

      B3SingleHeaderFormatter.setResponseHeaders({
        case B3Header => B3Header
      }, setHeader)

      verify(setHeader).apply(B3Header, B3Header)
      verifyNoMoreInteractions(setHeader)
    }

    def sampledFlag(sampled: Boolean, debug: Boolean): String = (sampled, debug) match {
      case (_, true) => "d"
      case (true, false) => "1"
      case (false, false) => "0"
    }
  }
}
