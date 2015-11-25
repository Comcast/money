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

package com.comcast.money.emitters

import akka.actor.Props
import akka.event.Logging
import akka.testkit.TestActorRef
import com.comcast.money.core.{ Note, Span, SpanId, StringNote }
import com.comcast.money.internal.EmitterProtocol.{ EmitMetricDouble, EmitSpan }
import com.comcast.money.test.AkkaTestJawn
import com.typesafe.config.{ Config, ConfigFactory }
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ BeforeAndAfterEach, WordSpecLike }
import org.slf4j.Logger

import scala.concurrent.duration._

class LogEmitterSpec extends AkkaTestJawn with WordSpecLike with BeforeAndAfterEach with MockitoSugar {

  val emitterConf = ConfigFactory.parseString(
    """
          {
            log-level="INFO"
            emitter="com.comcast.money.emitters.LogRecorder"
          }
    """
  )

  val mockLogger = mock[Logger]
  class TestLogEmitter(conf: Config) extends LogEmitter(conf) {

    override def record(message: String) = {
      mockLogger.info(message)
    }
  }

  override def beforeEach(): Unit = {
    LogRecord.clear()
  }

  "A LogEmitter must" must {
    "log request spans" in {
      val underTest = system.actorOf(LogEmitter.props(emitterConf))
      val sampleData = Span(
        SpanId(1L), "key", "unknown", "host", 1L, true, 35L,
        Map("what" -> Note("what", 1L), "when" -> Note("when", 2L), "bob" -> Note("bob", "craig"))
      )
      val span = EmitSpan(sampleData)
      val expectedLogMessage = LogEmitter.buildMessage(sampleData)

      underTest ! span
      expectLogMessageContaining(expectedLogMessage)
    }
    "have a correctly formatted message" in {
      val sampleData = Span(
        SpanId(1L), "key", "unknown", "host", 1L, true, 35L,
        Map("what" -> Note("what", 1L), "when" -> Note("when", 2L), "bob" -> Note("bob", "craig"))
      )
      val actualMessage = LogEmitter.buildMessage(sampleData)
      assert(
        actualMessage === ("Span: [ span-id=1 ][ trace-id=1 ][ parent-id=1 ][ span-name=key ][ app-name=unknown ][ " +
          "start-time=1 ][ span-duration=35 ][ span-success=true ][ bob=craig ][ what=1 ][ when=2 ]")
      )
    }
    "sample spans" in within(1 second) {
      val conf = ConfigFactory.parseString(
        """
          {
            log-level="INFO"
            emitter="com.comcast.money.emitters.LogRecorder"
            sampling {
              enabled = true
              class-name = "com.comcast.money.sampling.EveryNSampler"
              every = 2
            }
          }
        """
      )
      val sampledEmitter = TestActorRef(new TestLogEmitter(conf))
      val sampleData = Span(
        SpanId(1L), "key", "unknown", "host", 1L, true, 35L,
        Map("what" -> Note("what", 1L), "when" -> Note("when", 2L), "bob" -> Note("bob", "craig"))
      )
      val span = EmitSpan(sampleData)

      // first span is not recorded because we are sampling every other
      sampledEmitter ! span
      verifyZeroInteractions(mockLogger)

      // next span is recorded
      sampledEmitter ! span
      verify(mockLogger).info(anyString())
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
    "parse log level" in {
      LogEmitter.logLevel(emitterConf) shouldBe Logging.InfoLevel
    }
    "default the log level to warn if the log level is not found" in {
      val conf = ConfigFactory.parseString(
        """
          {
            log-level="FOO"
            emitter="com.comcast.money.emitters.LogRecorder"
          }
        """
      )
      LogEmitter.logLevel(conf) shouldBe Logging.WarningLevel
    }
    "default the log level to warn if the log level is not set" in {
      val conf = ConfigFactory.parseString(
        """
          {
            emitter="com.comcast.money.emitters.LogRecorder"
          }
        """
      )
      LogEmitter.logLevel(conf) shouldBe Logging.WarningLevel
    }
    "assume a default LogEmitter if the emitter class is not configured" in {
      val conf = ConfigFactory.parseString(
        """
          {
            log-level="FOO"
          }
        """
      )
      val props = LogEmitter.props(conf)
      props.actorClass() shouldBe classOf[LogEmitter]
    }
  }
}
