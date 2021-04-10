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

package com.comcast.money.otel.handlers

import com.comcast.money.api.{ IdGenerator, SpanInfo, SpanLink }
import io.opentelemetry.api.common.{ AttributeKey, Attributes }
import io.opentelemetry.api.trace.{ SpanContext, TraceFlags, TraceState }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class MoneyLinkSpec extends AnyWordSpec with Matchers {

  val linkedContext = SpanContext.create(IdGenerator.generateRandomTraceIdAsHex(), IdGenerator.generateRandomIdAsHex(), TraceFlags.getSampled, TraceState.getDefault)
  val link = new SpanLink {
    override def spanContext(): SpanContext = linkedContext
    override def attributes(): Attributes = Attributes.of(AttributeKey.stringKey("foo"), "bar")
  }

  "MoneyLink" should {
    "should wrap a Money link" in {
      val underTest = MoneyLink(link)

      underTest.getSpanContext shouldBe link.spanContext
      underTest.getAttributes shouldBe link.attributes
      underTest.getTotalAttributeCount shouldBe 1
    }
  }
}
