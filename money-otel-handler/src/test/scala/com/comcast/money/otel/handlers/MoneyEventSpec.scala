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

import com.comcast.money.api.{ EventInfo, SpanInfo }
import io.opentelemetry.api.common.{ AttributeKey, Attributes }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class MoneyEventSpec extends AnyWordSpec with Matchers {

  val event = new EventInfo {
    override def name(): String = "name"
    override def attributes(): Attributes = Attributes.of(AttributeKey.stringKey("foo"), "bar")
    override def timestampNanos(): Long = 1234567890L
    override def exception(): Throwable = null
  }

  "MoneyEvent" should {
    "wrap a Money Event" in {
      val underTest = MoneyEvent(event)

      underTest.getName shouldBe event.name()
      underTest.getEpochNanos shouldBe event.timestampNanos()
      underTest.getTotalAttributeCount shouldBe 1
      underTest.getAttributes shouldBe Attributes.of(AttributeKey.stringKey("foo"), "bar")
    }
  }
}
