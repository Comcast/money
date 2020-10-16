/*
 * Copyright 2012 Comcast Cable Communications Management, LLC
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

package com.comcast.money.core.handlers

import com.comcast.money.api.{ Note, SpanId }
import com.comcast.money.core.CoreSpanInfo
import com.typesafe.config.ConfigFactory
import io.opentelemetry.trace.StatusCanonicalCode

import scala.collection.JavaConverters._
import scala.collection._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

class SpanLogFormatterSpec extends AnyWordSpec with Matchers {

  val emitterConf = ConfigFactory.parseString(
    """
          {
            log-level="INFO"
            emitter="com.comcast.money.emitters.LogRecorder"
          }
    """)
  val spanLogFormatter = SpanLogFormatter(emitterConf)

  val spanId = new SpanId("01234567-890A-BCDE-F012-34567890ABCD", 81985529216486895L, 81985529216486895L)
  val sampleData = CoreSpanInfo(
    id = spanId,
    startTimeNanos = 1000000L,
    endTimeNanos = 26000000L,
    durationNanos = 35000000L,
    name = "key",
    appName = "unknown",
    host = "host",
    notes = Map[String, Note[_]]("bob" -> Note.of("bob", "craig"), "what" -> Note.of("what", 1L), "when" -> Note.of("when", 2L)).asJava,
    status = StatusCanonicalCode.OK)

  val withNull = CoreSpanInfo(
    id = spanId,
    startTimeNanos = 1000000L,
    endTimeNanos = 26000000L,
    durationNanos = 35000000L,
    name = "key",
    appName = "unknown",
    host = "host",
    notes = Map[String, Note[_]]("empty" -> Note.of("empty", null)).asJava,
    status = StatusCanonicalCode.OK)

  "A LogEmitter must" must {
    "have a correctly formatted message" in {
      val actualMessage = spanLogFormatter.buildMessage(sampleData)

      assert(
        actualMessage === ("Span: [ span-id=81985529216486895 ][ trace-id=01234567-890A-BCDE-F012-34567890ABCD ][ parent-id=81985529216486895 ]" +
          "[ span-name=key ][ app-name=unknown ][ start-time=1 ][ span-duration=35000 ][ span-success=true ][ bob=craig ][ what=1 ][ when=2 ]"))
    }
    "honor key names from the config" in {
      val conf = ConfigFactory.parseString(
        """
              {
                emitter="com.comcast.money.emitters.LogRecorder"
                formatting {
                  keys {
                    span-id = "spanId"
                    trace-id = "traceId"
                    parent-id = "parentId"
                    span-name = "spanName"
                    app-name = "appName"
                    start-time = "startTime"
                    span-duration = "spanDuration"
                    span-success = "spanSuccess"
                  }
                }
              }
        """)
      val spanLogFormatter = SpanLogFormatter(conf)

      val actualMessage = spanLogFormatter.buildMessage(sampleData)
      assert(
        actualMessage === ("Span: [ spanId=81985529216486895 ][ traceId=01234567-890A-BCDE-F012-34567890ABCD ][ parentId=81985529216486895 ]" +
          "[ spanName=key ][ appName=unknown ][ startTime=1 ][ spanDuration=35000 ][ spanSuccess=true ][ bob=craig ][ what=1 ][ when=2 ]"))
    }
    "honor the span-start from the config" in {
      val conf = ConfigFactory.parseString(
        """
              {
                emitter="com.comcast.money.emitters.LogRecorder"
                formatting {
                  span-start = "Start :|: "
                }
              }
        """)
      val spanLogFormatter = SpanLogFormatter(conf)
      val actualMessage = spanLogFormatter.buildMessage(sampleData)
      assert(
        actualMessage === ("Start :|: [ span-id=81985529216486895 ][ trace-id=01234567-890A-BCDE-F012-34567890ABCD ][ parent-id=81985529216486895 ]" +
          "[ span-name=key ][ app-name=unknown ][ start-time=1 ][ span-duration=35000 ][ span-success=true ][ bob=craig ][ what=1 ][ when=2 ]"))
    }
    "honor the log-template from the config" in {
      val conf = ConfigFactory.parseString(
        """
              {
                emitter="com.comcast.money.emitters.LogRecorder"
                formatting {
                  log-template = "%s=\"%s\" "
                }
              }
        """)
      val spanLogFormatter = SpanLogFormatter(conf)
      val actualMessage = spanLogFormatter.buildMessage(sampleData)
      assert(
        actualMessage === ("""Span: span-id="81985529216486895" trace-id="01234567-890A-BCDE-F012-34567890ABCD" parent-id="81985529216486895" """ +
          """span-name="key" app-name="unknown" start-time="1" span-duration="35000" span-success="true" """ +
          """bob="craig" what="1" when="2" """))
    }
    "honor the span-duration-ms settings in the config" in {
      val conf = ConfigFactory.parseString(
        """
              {
                emitter="com.comcast.money.emitters.LogRecorder"
                formatting {
                  span-duration-ms-enabled = "true"
                  keys {
                    span-duration-ms = "spanDurationMs"
                  }
                }
              }
        """)
      val spanLogFormatter = SpanLogFormatter(conf)
      val actualMessage = spanLogFormatter.buildMessage(sampleData)

      actualMessage should include("[ span-duration=35000 ]")
      actualMessage should include("[ spanDurationMs=35 ]")
    }
    "log NULL when the note value is None" in {
      val expectedLogMessage = spanLogFormatter.buildMessage(withNull)

      expectedLogMessage should include("[ empty=NULL ]")
    }
    "honor the null value to log from the config" in {
      val conf = ConfigFactory.parseString(
        """
              {
                emitter="com.comcast.money.emitters.LogRecorder"
                formatting {
                  null-value = "null_value"
                }
              }
        """)
      val spanLogFormatter = SpanLogFormatter(conf)
      val expectedLogMessage = spanLogFormatter.buildMessage(withNull)

      expectedLogMessage should include("[ empty=null_value ]")
    }
    "honor formatting span IDs as hex" in {
      val conf = ConfigFactory.parseString(
        """
              {
                emitter="com.comcast.money.emitters.LogRecorder"
                formatting {
                  format-ids-as-hex = true
                }
              }
        """)
      val spanLogFormatter = SpanLogFormatter(conf)
      val expectedLogMessage = spanLogFormatter.buildMessage(sampleData)

      expectedLogMessage should include("[ span-id=0123456789abcdef ][ trace-id=01234567890abcdef01234567890abcd ][ parent-id=0123456789abcdef ]")
    }
  }
}
