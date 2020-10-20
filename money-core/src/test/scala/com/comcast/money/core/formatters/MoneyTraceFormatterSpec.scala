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
import com.comcast.money.core.formatters.MoneyTraceFormatter.{ MoneyHeaderFormat, MoneyTraceHeader }
import com.comcast.money.core.{ Formatters, TraceGenerators }
import io.opentelemetry.trace.{ TraceFlags, TraceState }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class MoneyTraceFormatterSpec extends AnyWordSpec with Matchers with ScalaCheckDrivenPropertyChecks with TraceGenerators {

  "MoneyTraceFormatter" should {
    "read a money http header" in {
      forAll { (traceIdValue: UUID, parentSpanIdValue: Long, spanIdValue: Long) =>
        val expectedSpanId = SpanId.createRemote(traceIdValue.toString, parentSpanIdValue, spanIdValue, TraceFlags.getSampled, TraceState.getDefault)
        val spanId = MoneyTraceFormatter.fromHttpHeaders(
          getHeader = {
            case MoneyTraceHeader => MoneyHeaderFormat.format(expectedSpanId.traceId, expectedSpanId.parentId, expectedSpanId.selfId)
          })
        spanId shouldBe Some(expectedSpanId)
      }
    }

    "fail to read a badly formatted money http header" in {
      forAll { (traceIdValue: String, parentSpanIdValue: String, spanIdValue: String) =>
        val spanId = MoneyTraceFormatter.fromHttpHeaders(
          getHeader = {
            case MoneyTraceHeader => MoneyHeaderFormat.format(traceIdValue, parentSpanIdValue, spanIdValue)
          })
        spanId shouldBe None
      }
    }

    "create a money http header" in {
      forAll { (traceIdValue: UUID, parentSpanIdValue: Long, spanIdValue: Long) =>
        val spanId = SpanId.createRemote(traceIdValue.toString, parentSpanIdValue, spanIdValue, TraceFlags.getSampled, TraceState.getDefault)
        MoneyTraceFormatter.toHttpHeaders(spanId, (header, value) => {
          header shouldBe MoneyTraceHeader
          value shouldBe MoneyHeaderFormat.format(spanId.traceId, spanId.parentId, spanId.selfId)
        })
      }
    }
  }
}
