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
import com.comcast.money.core.formatters.B3MultiHeaderFormatter._
import com.comcast.money.core.TraceGenerators
import io.opentelemetry.trace.{ TraceFlags, TraceState }
import org.scalacheck.Test.Failed
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import FormatterUtils._

class B3MultiHeaderFormatterSpec extends AnyWordSpec with Matchers with ScalaCheckDrivenPropertyChecks with TraceGenerators {
  "read B3 headers correctly for any valid hex encoded headers: trace-Id , parent id and span ID" in {
    forAll { (traceIdValue: UUID, parentSpanIdValue: Long, spanIdValue: Long, sampled: Boolean) =>
      whenever(isValidIds(traceIdValue, parentSpanIdValue, spanIdValue)) {

        val expectedSpanId = SpanId.createRemote(traceIdValue.toString, parentSpanIdValue, spanIdValue, TraceFlags.getSampled, TraceState.getDefault)
        val spanId = B3MultiHeaderFormatter.fromHttpHeaders(
          getHeader = {
            case B3TraceIdHeader => traceIdValue.hex64or128
            case B3ParentSpanIdHeader => parentSpanIdValue.hex64
            case B3SpanIdHeader => spanIdValue.hex64
            case B3SampledHeader => if (sampled) "1" else "0"
            case B3FlagsHeader => null
          })
        spanId shouldBe Some(expectedSpanId)

        val maybeRootSpanId = B3MultiHeaderFormatter.fromHttpHeaders(
          getHeader = {
            case B3TraceIdHeader => traceIdValue.hex64or128
            case B3SpanIdHeader => spanIdValue.hex64
            case _ => null
          })
        val rootSpanId = maybeRootSpanId
        rootSpanId should not be None
        rootSpanId.get.traceId shouldBe traceIdValue.toString
        rootSpanId.get.parentId shouldBe spanIdValue
        rootSpanId.get.selfId shouldBe spanIdValue
      }
    }
  }

  "fail to read B3 headers correctly for invalid headers" in {
    val spanId = B3MultiHeaderFormatter.fromHttpHeaders(getHeader = _ => "garbage")
    spanId shouldBe None
  }

  "create B3 headers correctly given any valid character UUID for trace-Id and any valid long integers for parent and span ID, where if parent == span id, parent will not be emitted" in {
    forAll { (traceIdValue: UUID, parentSpanIdValue: Long, spanIdValue: Long, sampled: Boolean) =>
      whenever(isValidIds(traceIdValue, parentSpanIdValue, spanIdValue)) {

        val traceFlags = if (sampled) TraceFlags.getSampled else TraceFlags.getDefault
        val expectedSpanId = SpanId.createRemote(traceIdValue.toString, parentSpanIdValue, spanIdValue, traceFlags, TraceState.getDefault)
        B3MultiHeaderFormatter.toHttpHeaders(expectedSpanId, (k, v) => k match {
          case B3TraceIdHeader => v shouldBe traceIdValue.hex64or128
          case B3ParentSpanIdHeader if expectedSpanId.isRoot => Failed
          case B3ParentSpanIdHeader => v shouldBe parentSpanIdValue.hex64
          case B3SpanIdHeader => v shouldBe spanIdValue.hex64
          case B3SampledHeader if sampled => v shouldBe "1"
          case B3SampledHeader => v shouldBe "0"
        })
      }
    }
  }
}
