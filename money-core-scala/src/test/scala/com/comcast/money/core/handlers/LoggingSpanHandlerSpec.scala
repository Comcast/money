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

package com.comcast.money.core.handlers

import com.comcast.money.api.Note
import com.typesafe.config.ConfigFactory
import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ Matchers, OneInstancePerTest, WordSpec }
import org.slf4j.Logger

import scala.collection.JavaConversions._

class LoggingSpanHandlerSpec extends WordSpec
    with Matchers with MockitoSugar with OneInstancePerTest with TestData {

  val mockLogger = mock[Logger]
  val logEntryCaptor = ArgumentCaptor.forClass(classOf[String])
  val underTest = new LoggingSpanHandler(mockLogger)

  "LoggingSpanHandler" should {
    "log span info" in {
      underTest.handle(testSpanInfo)

      verify(mockLogger).info(logEntryCaptor.capture())

      val logEntry = logEntryCaptor.getValue

      logEntry should include(s"span-id=${testSpanInfo.id.selfId}")
      logEntry should include(s"parent-id=${testSpanInfo.id.parentId}")
      logEntry should include(s"trace-id=${testSpanInfo.id.traceId}")
      logEntry should include(s"span-duration=${testSpanInfo.durationMicros}")
      logEntry should include(s"start-time=${testSpanInfo.startTimeMillis}")
      logEntry should include(s"span-duration=${testSpanInfo.durationMicros}")
      logEntry should include(s"host=${testSpanInfo.host}")
      logEntry should include(s"app-name=${testSpanInfo.appName}")
      logEntry should include(s"span-name=${testSpanInfo.name}")
      logEntry should include(s"span-success=${testSpanInfo.success}")

      for (note <- testSpanInfo.notes.values) {
        logEntry should include(s"${note.name}=${note.value}")
      }
    }

    "log a NULL if the value of a note is null" in {

      val nullTest = testSpanInfo.copy(
        notes = Map("nullStr" -> Note.of("nullStr", null.asInstanceOf[String]))
      )

      underTest.handle(nullTest)

      verify(mockLogger).info(logEntryCaptor.capture())

      val logEntry = logEntryCaptor.getValue

      logEntry should include("nullStr=NULL")
    }

    "be configurable" in {
      underTest shouldBe a[ConfigurableHandler]
    }

    "be configured to use error" in {
      val config = ConfigFactory.parseString("log-level=ERROR")

      underTest.configure(config)
      underTest.handle(testSpanInfo)

      verify(mockLogger).error(logEntryCaptor.capture())
    }

    "be configured to use warn" in {
      val config = ConfigFactory.parseString("log-level=WARN")

      underTest.configure(config)
      underTest.handle(testSpanInfo)

      verify(mockLogger).warn(logEntryCaptor.capture())
    }

    "be configured to use info" in {
      val config = ConfigFactory.parseString("log-level=INFO")

      underTest.configure(config)
      underTest.handle(testSpanInfo)

      verify(mockLogger).info(logEntryCaptor.capture())
    }

    "be configured to use debug" in {
      val config = ConfigFactory.parseString("log-level=DEBUG")

      underTest.configure(config)
      underTest.handle(testSpanInfo)

      verify(mockLogger).debug(logEntryCaptor.capture())
    }

    "be configured to use trace" in {
      val config = ConfigFactory.parseString("log-level=TRACE")

      underTest.configure(config)
      underTest.handle(testSpanInfo)

      verify(mockLogger).trace(logEntryCaptor.capture())
    }

    "be configured to default to info" in {
      val config = ConfigFactory.parseString("no-configure=true")

      underTest.configure(config)
      underTest.handle(testSpanInfo)

      verify(mockLogger).info(logEntryCaptor.capture())
    }
  }
}
