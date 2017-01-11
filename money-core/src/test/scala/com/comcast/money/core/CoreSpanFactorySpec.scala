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

import com.comcast.money.api.{ Tag, SpanHandler, SpanId }
import com.comcast.money.core.handlers.TestData
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ Matchers, WordSpec }

class CoreSpanFactorySpec extends WordSpec with Matchers with MockitoSugar with TestData {

  val handler = mock[SpanHandler]
  val underTest = new CoreSpanFactory(handler)

  "CoreSpanFactory" should {
    "create a new span" in {
      val result = underTest.newSpan("foo").asInstanceOf[CoreSpan]

      result.info.name shouldBe "foo"
      result.handler shouldBe handler
    }

    "create a new span given an existing span id" in {
      val existingId = new SpanId()
      val result = underTest.newSpan(existingId, "foo").asInstanceOf[CoreSpan]

      result.id shouldBe existingId
    }

    "create a child span whos id descends from an existing span" in {
      val result = underTest.childSpan("child", testSpan)

      val parent = testSpan.info
      val child = result.info

      child.id.traceId shouldBe parent.id.traceId
      child.id.parentId shouldBe parent.id.selfId
    }

    "propagate sticky tags to a child span" in {

      val parentSpan = underTest.newSpan("parent")
      val stickyNote = Tag.of("foo", "bar", true)
      val nonStickyNote = Tag.of("other", "one", false)
      parentSpan.record(stickyNote)
      parentSpan.record(nonStickyNote)

      val childSpan = underTest.childSpan("child", parentSpan, true)
      val childInfo = childSpan.info

      childInfo.tags should contain value stickyNote
      childInfo.tags shouldNot contain value nonStickyNote
    }
  }
}
