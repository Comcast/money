package com.comcast.money.emitters

import akka.testkit.{TestActorRef, TestKit}
import com.comcast.money.core.{LongNote, Note, Span, SpanId}
import com.comcast.money.internal.EmitterProtocol.{EmitMetricDouble, EmitMetricLong, EmitSpan}
import com.comcast.money.test.AkkaTestJawn
import com.comcast.money.util.DateTimeUtil
import com.typesafe.config.Config
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, WordSpecLike}

class GraphiteEmitterSpec extends AkkaTestJawn with WordSpecLike with BeforeAndAfter with MockitoSugar {

  private val conf = mock[Config]

  before {
    DateTimeUtil.timeProvider = () => 1L

    doReturn(1).when(conf).getInt("emitterPoolSize")
  }

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
    DateTimeUtil.timeProvider = DateTimeUtil.SystemMicroTimeProvider
  }

  "A GraphiteEmitter" when {
    "sent a Metric" should {
      "emit metrics" in {
        val graphiteEmitter = TestActorRef(new GraphiteEmitter(conf) with TestProbeMaker)
        val metric = EmitMetricDouble("cherry.pie", 1.0)
        graphiteEmitter ! metric
        child(graphiteEmitter, "graphite-router").expectMsg(metric)
      }
    }
    "sent a SpanData" should {
      "emit a metric for all \"timings\"" in {
        val graphiteEmitter = TestActorRef(new GraphiteEmitter(conf) with TestProbeMaker)
        val span = Span(
          SpanId(1L), "happy span", "unknown", "localhost", 1L, true, 35L, Map(
            "when" -> Note("when", 1.5), "who" -> LongNote("who", None, 45), "bob" -> Note("bob", "1.2"),
            "apple" -> Note("apple", "pie")))
        graphiteEmitter ! EmitSpan(span)
        val msgs = child(graphiteEmitter, "graphite-router").receiveN(3)

        assert(msgs.contains(EmitMetricDouble("request-tracing-data.happy_span.when", 1.5, 1L)))
        assert(msgs.contains(EmitMetricLong("request-tracing-data.happy_span.who", 45, 45)))
        assert(msgs.contains(EmitMetricLong("request-tracing-data.happy_span.span-duration", 35, 1L)))
      }
      "emit a metric for all data" in {
        val graphiteEmitter = TestActorRef(new GraphiteEmitter(conf) with TestProbeMaker)
        val span = Span(
          SpanId(1L), "happy span", "unknown", "localhost", 1L, true, 35L, Map(
            "bob" -> Note("bob", 12.1), "apple" -> Note("apple", "pie"), "cherry" -> Note("cherry", 1.032)))
        graphiteEmitter ! EmitSpan(span)
        val msgs = child(graphiteEmitter, "graphite-router").receiveN(3)

        assert(msgs.contains(EmitMetricDouble("request-tracing-data.happy_span.cherry", 1.032, 1L)))
        assert(msgs.contains(EmitMetricDouble("request-tracing-data.happy_span.bob", 12.1, 1L)))

        child(graphiteEmitter, "graphite-router").expectNoMsg()
      }
      "emit a metric for longs" in {
        val graphiteEmitter = TestActorRef(new GraphiteEmitter(conf) with TestProbeMaker)
        val span = Span(
          SpanId(1L), "happy span", "unknown", "localhost", 1L, true, 35L, Map("long" -> Note("long", 15L)))
        graphiteEmitter ! EmitSpan(span)
        val msgs = child(graphiteEmitter, "graphite-router").receiveN(2)

        assert(msgs.contains(EmitMetricLong("request-tracing-data.happy_span.long", 15L, 1L)))
      }
    }
    "cover code" should {
      "loves us some code coverage" in {
        val props = GraphiteEmitter.props(conf)
        props.actorClass shouldEqual classOf[GraphiteEmitter]
      }
    }
  }
}
