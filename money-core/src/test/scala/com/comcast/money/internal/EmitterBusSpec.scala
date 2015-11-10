package com.comcast.money.internal

import akka.testkit.TestProbe
import com.comcast.money.core.{Note, Span, SpanId}
import com.comcast.money.internal.EmitterBus.{EmitterEvent, EmitterGroup}
import com.comcast.money.internal.EmitterProtocol.EmitSpan
import com.comcast.money.test.AkkaTestJawn
import org.scalatest.WordSpecLike
import org.scalatest.mock.MockitoSugar

class EmitterBusSpec extends AkkaTestJawn with WordSpecLike with MockitoSugar {

  "An EmitterBus" when {
    val underTest = new EmitterBus()
    val testData = Span(
      SpanId(1L), "happy span", "app", "host", 1L, true, 3L, Map(
        "when" -> Note("when", 1L), "who" -> Note("who", 2L), "bob" -> Note("bob", "1.2"),
        "apple" -> Note("apple", "pie")))

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
