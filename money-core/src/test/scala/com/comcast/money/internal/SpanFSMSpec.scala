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

import akka.actor.Props
import akka.pattern.ask
import akka.testkit._
import akka.util.Timeout
import com.comcast.money.api.SpanId
import com.comcast.money.core._
import com.comcast.money.internal.EmitterProtocol.EmitSpan
import com.comcast.money.internal.SpanFSM.{ NoteWrapper, SpanContext }
import com.comcast.money.internal.SpanSupervisorProtocol.SpanMessage
import com.comcast.money.test.AkkaTestJawn
import com.comcast.money.util.DateTimeUtil
import org.scalatest.{ BeforeAndAfter, GivenWhenThen, WordSpecLike }

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration._

class SpanFSMSpec extends AkkaTestJawn with WordSpecLike with BeforeAndAfter with GivenWhenThen with ImplicitSender {

  import com.comcast.money.internal.SpanFSMProtocol._

  implicit val askTimeout = new Timeout(500.millis)

  before {
    val times = mutable.Stack(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L)
    DateTimeUtil.timeProvider = () => times.pop()
  }

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
    DateTimeUtil.timeProvider = DateTimeUtil.SystemMicroTimeProvider
  }

  "A Span FSM" should {
    "accept data and then emit" in {
      val span = system.actorOf(Props(classOf[SpanFSM], testActor))
      Given("A Start")
      span ! Start(new SpanId("foo", 1L, 1L), "happy span", parentSpanId = Some(new SpanId("foo", 2L, 2L)))
      Then("it should sent a PropagateNotesReq to its parent")
      expectMsg(SpanMessage(new SpanId("foo", 2L, 2L), PropagateNotesRequest(span)))

      When("it receives a PropagateNotesReq ")
      val notesPropogationProbe = TestProbe()
      span ! AddNote(Note("where", "Philly", 2L), true)
      span ! AddNote(Note("who", "tom", 2L))
      span ! PropagateNotesRequest(notesPropogationProbe.ref)

      Then("it should send its propogate-able notes")
      notesPropogationProbe
        .expectMsg(PropagateNotesResponse(Map("where" -> NoteWrapper(Note("where", "Philly", 2L), true))))

      When("it receives timing data messages and finally a Stop")
      span ! PropagateNotesResponse(
        Map(
          "huh" -> NoteWrapper(Note("huh", "Baltimore", 2L)),
          "whowhat" -> NoteWrapper(Note("whowhat", "Wilmington", 2L))
        )
      )

      span ! Stop(true, 3L)

      Then("it should emit a span record")
      expectMsg(
        EmitSpan(
          Span(
            new SpanId("foo", 1L, 1L), "happy span", Money.applicationName, Money.hostName, 1L, true, 2L, Map(
              "whowhat" -> Note("whowhat", "Wilmington", 2L), "huh" -> Note("huh", "Baltimore", 2L),
              "where" -> Note("where", "Philly", 2L), "who" -> Note("who", "tom", 2L)
            )
          )
        )
      )
    }

    "accept data after a stop" in {
      val span = system.actorOf(Props(classOf[SpanFSM], testActor))
      Given("a Start and Stop")
      span ! Start(new SpanId("foo", 1L, 1L), "happy span")
      span ! Stop(Result.success, 2L)

      When("timing and data are sent")
      span ! AddNote(Note("who", "tom", 3L))

      When("it receives a PropagateNotesReq ")
      val notesPropogationProbe = TestProbe()
      span ! AddNote(Note("where", "Philly", 2L), true)
      span ! PropagateNotesRequest(notesPropogationProbe.ref)

      Then("it should send its propogate-able notes")
      notesPropogationProbe
        .expectMsg(PropagateNotesResponse(Map("where" -> NoteWrapper(Note("where", "Philly", 2L), true))))

      Then("after a second it should Emit")
      Thread.sleep(1100) //force TimeOut Event while Stopped
      expectMsg(
        EmitSpan(
          Span(
            new SpanId("foo", 1L, 1L), "happy span", Money.applicationName, Money.hostName, 1L, true, 1L,
            Map("where" -> Note("where", "Philly", 2L), "who" -> Note("who", "tom", 3L))
          )
        )
      )

      Then("it should ignore further messages")
      span ! AddNote(Note("what", "monkey", 3L))
      expectNoMsg(2 seconds)
    }

    "timeout and not emit if it never receives a Stop" in {
      Given("A Start")
      val span = system.actorOf(Props(classOf[SpanFSM], testActor, 1 second, 1 second))
      span ! Start(new SpanId("foo", 1L, 1L), "happy span")
      When("No further messages for over a second")
      Thread.sleep(1100)
      Then("it stops and does not emit a SpanData ")
      expectNoMsg(1 seconds)
    }

    "log a warning for an unexpected event" in {
      Given("A Start")
      val span = system.actorOf(Props(classOf[SpanFSM], testActor, 1 second, 1 second))
      span ! Start(new SpanId("foo", 1L, 1L), "happy span")
      When("an unexpected event is sent to the request span")

      Then("a log message is captured")
      EventFilter.warning(pattern = "Received unknown event*", occurrences = 1) intercept {
        span ! "foo"
      }
    }

    "start a timer when it receives a start timer message" in {

      Given("A span has started")
      val span = system.actorOf(Props(classOf[SpanFSM], testActor, 1 second, 1 second))
      span ! Start(new SpanId("foo", 1L, 1L), "happy span")

      When("a StartTimer message is sent")
      span ! StartTimer("timer-test", 1L)

      Then("a timer has been added to the span context")
      val fut = span ? Query
      val ctx = Await.result(fut, 100 millis).asInstanceOf[SpanContext]
      println(ctx)
      ctx.timers should contain("timer-test", 1L)
    }

    "add a note for a timer" in {

      Given("A span has started")
      val span = system.actorOf(Props(classOf[SpanFSM], testActor, 1 second, 1 second))
      span ! Start(new SpanId("foo", 1L, 1L), "happy span")

      And("a timer has been started")
      span ! StartTimer("timer-test", 1L)

      When("a stop timer message is sent")
      span ! StopTimer("timer-test", 4L)

      Then("a note is added to the span context for the timer")
      val fut = span ? Query
      val ctx = Await.result(fut, 100 millis).asInstanceOf[SpanContext]
      ctx.notes should contain key "timer-test"

      And("the note for the timer contains the difference between the start and end time")
      ctx.notes should contain("timer-test", NoteWrapper(Note("timer-test", 3L, 1)))

      println(ctx.notes)
    }

    "emit any unstopped timers when it stops" in {

      Given("A span has started")
      val span = TestActorRef(Props(classOf[SpanFSM], testActor, 1 second, 1 second))
      span ! Start(new SpanId("foo", 1L, 1L), "happy span")

      And("a timer has been started")
      span ! StartTimer("timer-test", 1L)

      When("the span is stopped")
      span ! Stop(true, 2L)

      Then("any unstopped timers are stopped and removed from the span context")
      val spanState = span.underlyingActor.asInstanceOf[SpanFSM].stateData.asInstanceOf[SpanContext]
      spanState.timers shouldBe empty

      And("a note for the timer that was never stopped exists in the span that is emitted")
      expectMsgPF() {
        case EmitSpan(Span(spanId, name, app, host, startTime, success, duration, notes)) =>
          notes should contain("timer-test", Note("timer-test", 1L, 1))
      }
    }

    "allow multiple stops" in {

      Given("a span has started")
      val span = TestActorRef(Props(classOf[SpanFSM], testActor, 1 second, 1 second))
      span ! Start(new SpanId("foo", 1L, 1L), "happy span")

      And("the span is stopped")
      span ! Stop(true, 2L)

      When("another stop message arrives sometime later")
      span ! Stop(false, 4L)

      And("the span is finished and emitted")
      Then("the span duration that is emitted includes the time from the start to the last stop message")
      And("the result that is recorded is the last one that got in")

      expectMsgPF() {
        case EmitSpan(Span(spanId, name, app, host, startTime, success, duration, notes)) =>
          duration shouldBe 3
          success shouldBe false
      }
    }
  }

  "store the result provided in a stop span" in {
    val span = system.actorOf(Props(classOf[SpanFSM], testActor))
    Given("a Start and Stop with a result")
    span ! Start(new SpanId("foo", 1L, 1L), "happy span")
    span ! Stop(true, 2L)

    When("the span data is emitted")
    Then("the span data contains the result passed in on the Stop message")
    expectMsg(EmitSpan(Span(new SpanId("foo", 1L, 1L), "happy span", Money.applicationName, Money.hostName, 1L, true, 1L, Map())))
  }

  "SpanFSM props" should {
    "read the config value from the config file for span time out" in {
      val sampleSpan = TestActorRef(SpanFSM.props(testActor))
      sampleSpan.underlyingActor.asInstanceOf[SpanFSM].openSpanTimeout shouldEqual 60.seconds
    }
  }
}
