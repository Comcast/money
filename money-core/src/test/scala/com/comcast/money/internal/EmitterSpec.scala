package com.comcast.money.internal

import akka.testkit.TestActorRef
import com.comcast.money.test.AkkaTestJawn
import com.comcast.money.core.{Span, SpanId}
import com.comcast.money.internal.EmitterProtocol.{EmitMetricLong, EmitMetricDouble, EmitSpan}
import com.comcast.money.util.DateTimeUtil
import org.scalatest.WordSpecLike
import org.scalatest.mock.MockitoSugar

class EmitterSpec extends AkkaTestJawn with WordSpecLike with MockitoSugar {

  "An Emitter" when {
    val emitterBus = new EmitterBus()
    val underTest = TestActorRef(new Emitter(emitterBus) with TestProbeMaker)

    "sending a span message" should {
      val data = Span(SpanId(1L), "record", "app", "host", 2L, true, 35L, Map())
      val span = EmitSpan(data)
      underTest ! span

      "deliver the message to all children" in {
        child(underTest, "graphite-emitter").expectMsg(span)
        child(underTest, "log-emitter").expectMsg(span)
      }
    }
    "sending a metric" should {
      val metric = EmitMetricDouble("path", 1.0)
      underTest ! metric

      "only deliver the message to graphite" in {
        child(underTest, "graphite-emitter").expectMsg(metric)
        child(underTest, "log-emitter").expectNoMsg()
      }
    }
    "sending a metric long" should {
      val metric = EmitMetricLong("path", 2L)
      underTest ! metric

      "only deliver the message to graphite" in {
        child(underTest, "graphite-emitter").expectMsg(metric)
        child(underTest, "log-emitter").expectNoMsg()
      }
    }
    "getting props" should {
      val props = Emitter.props()
      props.actorClass() shouldBe a[Class[Emitter]]
    }
  }
  "Creating an EmitMetric instance" should {
    "not divide the timestamp by 1000" in {
      DateTimeUtil.timeProvider = () => 1000L
      val em = EmitMetricDouble("path", 1.0)
      em.timestamp shouldEqual 1000L
    }
  }
}
