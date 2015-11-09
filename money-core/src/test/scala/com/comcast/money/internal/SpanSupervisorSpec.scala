package com.comcast.money.internal

import akka.actor._
import akka.testkit.{TestProbe, EventFilter, TestActorRef}
import com.comcast.money.test.AkkaTestJawn
import com.comcast.money.core.{LongNote, Note, SpanId}
import com.comcast.money.internal.SpanFSMProtocol._
import com.comcast.money.internal.SpanSupervisorProtocol.SpanMessage
import org.scalatest.WordSpecLike

import scala.concurrent.duration._

class SpanSupervisorSpec extends AkkaTestJawn with WordSpecLike {

  val testEmitter = TestActorRef[Emitter](Emitter.props(), "emitter")
  val testFingerprint = SpanId(1L)
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
      spanSupervisor ! SpanMessage(testFingerprint, AddNote(LongNote("bob", None, 1)))

      "forward the span data to the request span child" in {
        child(spanSupervisor, testFingerprint.toString).expectMsg(AddNote(LongNote("bob", None, 1)))
      }
    }
    "sending span data to a non-existing request span" should {
      "log a message that data was sent to a non-existing span" in {
        EventFilter.warning(pattern = "Attempted to message non-existent SpanFSM*", occurrences = 1) intercept {
          spanSupervisor ! SpanMessage(SpanId(2L), AddNote(Note("ben")))
        }
      }
    }
    "sending a Propagate command" should {
      "not log a message if the span doesn't exist" in {
        val probe = TestProbe()
        spanSupervisor ! SpanMessage(SpanId(2L), PropagateNotesRequest(probe.ref))

        probe.expectNoMsg(1.seconds)
      }
    }
  }
}