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

import akka.testkit.TestProbe
import com.comcast.money.api.SpanId
import com.comcast.money.internal.EmitterProtocol.{ EmitMetricLong, EmitMetricDouble }
import com.comcast.money.internal.SpanFSMProtocol._
import com.comcast.money.internal.SpanLocal
import com.comcast.money.internal.SpanSupervisorProtocol.SpanMessage
import com.comcast.money.test.AkkaTestJawn
import com.comcast.money.util.DateTimeUtil
import org.scalatest._

class TracerSpec
    extends AkkaTestJawn with FeatureSpecLike with GivenWhenThen with BeforeAndAfterEach with OneInstancePerTest {

  override def beforeEach() {
    SpanLocal.clear()
    DateTimeUtil.timeProvider = () => 999L
  }

  val emitter = new TestProbe(system) {
    def expectEmitterMsg(em: EmitMetricDouble): Unit = {
      expectMsgPF() {
        case EmitMetricDouble(path, metricValue, timestamp) =>
          em.metricPath shouldEqual path
          em.value shouldEqual metricValue
      }
    }

    def expectEmitterMsg(em: EmitMetricLong): Unit = {
      expectMsgPF() {
        case EmitMetricLong(path, metricValue, timestamp) =>
          em.metricPath shouldEqual path
          em.value shouldEqual metricValue
      }
    }
  }

  val spanSupervisor = new TestProbe(system) {
    def expectSpanMsg(tm: SpanCommand) {
      expectMsgPF() {
        case SpanMessage(spanId, Stop(result, timeStamp)) =>
          tm shouldBe a[Stop]
          val tmResult = tm.asInstanceOf[Stop].result
          tmResult shouldEqual result
        case SpanMessage(spanId, Start(innerSpanId, spanName, timeStamp, _)) =>
          tm shouldBe a[Start]
          tm.asInstanceOf[Start].spanName shouldEqual spanName
        case SpanMessage(spanId, StartTimer(name, startTime)) =>
          tm shouldBe a[StartTimer]
          val tmStart = tm.asInstanceOf[StartTimer]
          tmStart.name shouldEqual name
        case SpanMessage(spanId, StopTimer(name, stopTime)) =>
          tm shouldBe a[StopTimer]
          val tmStop = tm.asInstanceOf[StopTimer]
          tmStop.name shouldEqual name
        case SpanMessage(spanId, AddNote(note, _)) =>
          tm shouldBe an[AddNote]
          val tmNote = tm.asInstanceOf[AddNote]
          tmNote.note.name shouldEqual note.name
          tmNote.note.value shouldEqual note.value
      }
    }
  }

  val moneyTracer = new Tracer {
    val spanSupervisorRef = spanSupervisor.ref
  }
  val moneyMetrics = new Metrics {
    val emitterRef = emitter.ref
  }

  feature("Span capturing") {

    scenario("Span captured with no Span context") {
      moneyTracer.startSpan("bob")
      val spanId = SpanLocal.current.get
      moneyTracer.time("mike")
      moneyTracer.record("adam", "sneakers")
      moneyTracer.stopSpan()
      Then("Span messages should be sent to the spanSupervisor")
      spanSupervisor.expectSpanMsg(Start(new SpanId("foo", 1L), "bob"))
      spanSupervisor.expectSpanMsg(AddNote(LongNote("mike", Some(999L))))
      spanSupervisor.expectSpanMsg(AddNote(Note("adam", "sneakers")))
      spanSupervisor.expectSpanMsg(Stop(Result.success))
      And("the current spanId and parentSpanId should all be the same")
      spanId.parentId should be(spanId.selfId)
      And("SpanLocal should be empty after span completes")
      SpanLocal.current.isEmpty shouldBe true
    }

    scenario("Trace captured with no Trace context with close instead of stopTrace") {
      moneyTracer.startSpan("bob")
      val traceId = SpanLocal.current.get
      moneyTracer.time("mike")
      moneyTracer.record("adam", "sneakers")
      moneyTracer.close()
      Then("Trace messages should be sent to the traceSupervisor")
      spanSupervisor.expectSpanMsg(Start(new SpanId("foo", 1L), "bob"))
      spanSupervisor.expectSpanMsg(AddNote(LongNote("mike", Some(999L))))
      spanSupervisor.expectSpanMsg(AddNote(Note("adam", "sneakers")))
      spanSupervisor.expectSpanMsg(Stop(Result.success))
      And("the current spanId and parentSpanId should be the same")
      traceId.parentId should be(traceId.selfId)
      And("SpanLocal should be empty after trace completes")
      SpanLocal.current.isEmpty shouldBe true
    }

    scenario("A subspan is created") {
      moneyTracer.startSpan("bob")
      val parentSpan = SpanLocal.current.get
      moneyTracer.startSpan("henry")
      val currentSpan = SpanLocal.current.get

      Then("the current span should not be the same as the parentSpan")
      currentSpan should not be parentSpan
      And("the current span's parentSpanId should be teh parentSpan.spanId")
      currentSpan.parentId should be(parentSpan.selfId)
      And("the current span's traceId should be the same as the parentSpan.originSpanId")
      currentSpan.traceId should be(parentSpan.traceId)
    }

    scenario("a span has a timer started") {
      moneyTracer.startSpan("bob")
      moneyTracer.startTimer("timer-test")

      Then("the start message is sent to the span supervisor")
      spanSupervisor.expectSpanMsg(Start(new SpanId("foo", 1L), "bob"))
      spanSupervisor.expectSpanMsg(StartTimer("timer-test"))
    }
    scenario("a Span has a timer started and stopped") {
      moneyTracer.startSpan("bob")
      moneyTracer.startTimer("timer-test")
      moneyTracer.stopTimer("timer-test")

      Then("the stop message is sent to the span supervisor")
      spanSupervisor.expectSpanMsg(Start(new SpanId("foo", 1L), "bob"))
      spanSupervisor.expectSpanMsg(StartTimer("timer-test"))
      spanSupervisor.expectSpanMsg(StopTimer("timer-test"))
    }
  }
  feature("metric capturing") {
    scenario("individual metric is sent") {
      moneyMetrics.sendMetric("bob", 1.03)
      Then("The Emitter should receive an EmitMetric message")
      emitter.expectEmitterMsg(EmitMetricDouble("bob", 1.03))
    }
    scenario("individual metric is sent as a long") {
      moneyMetrics.sendMetric("bob", 300L)
      Then("The Emitter should receive an EmitMetric message")
      emitter.expectEmitterMsg(EmitMetricLong("bob", 300L))
    }
  }
  feature("recording the result of a span") {
    scenario("stopping a span with a failed result") {
      moneyTracer.startSpan("foo")
      moneyTracer.stopSpan(Result.failed)
      spanSupervisor.expectSpanMsg(Start(new SpanId("foo", 1L), "foo"))

      Then("the failed result is sent along with the stop message")
      spanSupervisor.expectSpanMsg(Stop(false))
    }
    scenario("stopping a span with a success result") {
      moneyTracer.startSpan("foo")
      moneyTracer.stopSpan(Result.success)
      spanSupervisor.expectSpanMsg(Start(new SpanId("foo", 1L), "foo"))

      Then("the failed result is sent along with the stop message")
      spanSupervisor.expectSpanMsg(Stop(true))
    }
  }
  feature("recording a Note on a span") {
    scenario("sending a note on an existing trace") {
      moneyTracer.startSpan("foo")
      moneyTracer.record(Note("foo", "bar"))
      moneyTracer.stopSpan(Result.success)

      spanSupervisor.expectSpanMsg(Start(new SpanId("foo", 1L), "foo"))

      Then("the note is sent to the span supervisor")
      spanSupervisor.expectSpanMsg(AddNote(Note("foo", "bar")))
    }
    scenario("sending a note when no trace exists") {
      moneyTracer.record(Note("foo", "bar"))

      Then("no messages are sent to the span supervisor")
      spanSupervisor.expectNoMsg()
    }
  }
}
