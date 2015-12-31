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

import akka.actor._
import akka.testkit.{ EventFilter, TestActorRef, TestProbe }
import com.comcast.money.api.{ Note, SpanId }
import com.comcast.money.internal.SpanFSMProtocol._
import com.comcast.money.internal.SpanSupervisorProtocol.SpanMessage
import com.comcast.money.test.AkkaTestJawn
import org.scalatest.WordSpecLike

import scala.concurrent.duration._

class SpanSupervisorSpec extends AkkaTestJawn with WordSpecLike {

  val testEmitter = TestActorRef[Emitter](Emitter.props(), "emitter")
  val testFingerprint = new SpanId("foo", 1L)
  val testMessage = "MSG TEST"

  "A SpanSupervisor" when {
    val spanSupervisor = TestActorRef(Props(new SpanSupervisor(testEmitter) with TestProbeMaker))

    "starting a span" should {
      spanSupervisor ! SpanMessage(testFingerprint, Start(testFingerprint, testMessage, 1L))

      "create a child" in {
        children(spanSupervisor) should have size 1
      }
      "send a start span message to the child" in {
        child(spanSupervisor, testFingerprint.toString).expectMsg(Start(testFingerprint, testMessage, 1L))
      }
    }

    "sending span data to an existing request span" should {
      spanSupervisor ! SpanMessage(testFingerprint, AddNote(Note.of("bob", "foo", 1L)))

      "forward the span data to the request span child" in {
        child(spanSupervisor, testFingerprint.toString).expectMsg(AddNote(Note.of("bob", "foo", 1L)))
      }
    }
    "sending span data to a non-existing request span" should {
      "log a message that data was sent to a non-existing span" in {
        EventFilter.warning(pattern = "Attempted to message non-existent SpanFSM*", occurrences = 1) intercept {
          spanSupervisor ! SpanMessage(new SpanId("foo", 2L), AddNote(Note.of("ben", "foo")))
        }
      }
    }
    "sending a Propagate command" should {
      "not log a message if the span doesn't exist" in {
        val probe = TestProbe()
        spanSupervisor ! SpanMessage(new SpanId("foo", 2L), PropagateNotesRequest(probe.ref))

        probe.expectNoMsg(1.seconds)
      }
    }
  }
}
