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

package com.comcast.money.internal

import akka.testkit.TestProbe
import com.comcast.money.api.{ Note, SpanId }
import com.comcast.money.core.Span
import com.comcast.money.internal.EmitterBus.{ EmitterEvent, EmitterGroup }
import com.comcast.money.internal.EmitterProtocol.EmitSpan
import com.comcast.money.test.AkkaTestJawn
import org.scalatest.WordSpecLike
import org.scalatest.mock.MockitoSugar

class EmitterBusSpec extends AkkaTestJawn with WordSpecLike with MockitoSugar {

  "An EmitterBus" when {
    val underTest = new EmitterBus()
    val testData = Span(
      new SpanId("foo", 1L), "happy span", "app", "host", 1L, true, 3L, Map(
        "when" -> Note.of("when", 1L), "who" -> Note.of("who", 2L), "bob" -> Note.of("bob", "1.2"),
        "apple" -> Note.of("apple", "pie")
      )
    )

    "calling classify" should {
      "return EmitterGroup" in {
        underTest.classify(EmitterEvent(EmitterBus.Trace, EmitSpan(testData))) shouldBe an[EmitterGroup]
      }
    }

    "calling publish" should {
      val testProbe = TestProbe()
      val testSubscriber = testProbe.ref
      underTest.subscribe(testSubscriber, EmitterBus.Trace)
      underTest.publish(EmitterEvent(EmitterBus.Trace, EmitSpan(testData)))

      "send the event data to the subscriber" in {
        testProbe.expectMsg(EmitSpan(testData))
      }
    }
  }
}
