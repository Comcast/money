package com.comcast.money.emitters

import com.comcast.money.core.{Note, Span, SpanId, StringNote}
import com.comcast.money.internal.EmitterProtocol.{EmitMetricDouble, EmitSpan}
import com.comcast.money.test.AkkaTestJawn
import com.typesafe.config.ConfigFactory
import org.scalatest.WordSpecLike

class LogEmitterSpec extends AkkaTestJawn with WordSpecLike {

  val emitterConf = ConfigFactory.parseString(
    """
          {
            emitter="com.comcast.money.emitters.LogRecorder"
          }
    """
  )

  "A LogEmitter must" must {
    "log request spans" in {
      val underTest = system.actorOf(LogEmitter.props(emitterConf))
      val sampleData = Span(SpanId(1L), "key", "unknown", "host", 1L, true, 35L, Map("what" -> Note("what", 1L), "when" -> Note("when", 2L), "bob" -> Note("bob", "craig")))
      val span = EmitSpan(sampleData)
      val expectedLogMessage = LogEmitter.buildMessage(sampleData)

      underTest ! span
      expectLogMessageContaining(expectedLogMessage)
    }
    "have a correctly formatted message" in {
      val sampleData = Span(SpanId(1L), "key", "unknown", "host", 1L, true, 35L, Map("what" -> Note("what", 1L), "when" -> Note("when", 2L), "bob" -> Note("bob", "craig")))
      val actualMessage = LogEmitter.buildMessage(sampleData)
      assert(actualMessage === ("Span: [ span-id=1 ][ trace-id=1 ][ parent-id=1 ][ span-name=key ][ app-name=unknown ][ start-time=1 ][ span-duration=35 ][ span-success=true ][ bob=craig ][ what=1 ][ when=2 ]"))
    }
    "log metrics" in {
      val underTest = system.actorOf(LogEmitter.props(emitterConf))
      underTest ! EmitMetricDouble("bob", 1.0)
      expectLogMessageContaining("bob=1.0")
    }
    "log NULL when the note value is None" in {
      val sampleData = Span(SpanId(1L), "key", "app", "host", 1L, true, 35L, Map("empty" -> StringNote("empty", None)))
      val expectedLogMessage = LogEmitter.buildMessage(sampleData)

      expectedLogMessage should include("[ empty=NULL ]")
    }
  }
}
