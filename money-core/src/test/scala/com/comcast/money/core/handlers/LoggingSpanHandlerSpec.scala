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

import com.comcast.money.api.SpanInfo
import com.typesafe.config.ConfigFactory
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.OneInstancePerTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.slf4j.Logger

class LoggingSpanHandlerSpec extends AnyWordSpec
  with Matchers with MockitoSugar with OneInstancePerTest with TestData {

  val mockLogger = mock[Logger]
  val mockLogFunction = mock[String => Unit]
  val mockFormatter = mock[SpanLogFormatter]
  val mockMakeFormatter = mock[SpanLogFormatter]
  val sampleMessage = "sample formatted log message"

  when(mockFormatter.buildMessage(any[SpanInfo])).thenReturn(sampleMessage)

  val logEntryCaptor = ArgumentCaptor.forClass(classOf[String])
  val underTest = new LoggingSpanHandler(mockLogFunction, mockFormatter)

  "LoggingSpanHandler" should {
    "log span info" in {
      val underTest = new LoggingSpanHandler(mockLogFunction, mockFormatter)
      underTest.handle(testSpanInfo)

      verify(mockFormatter).buildMessage(testSpanInfo)
    }

    "be configured to use error" in {
      val config = ConfigFactory.parseString(
        """
          |log-level=ERROR
          |formatting {
          | this=that
          |}
        """.stripMargin)

      val underTest = LoggingSpanHandler(mockLogger, config)
      underTest.handle(testSpanInfo)

      verify(mockLogger).error(logEntryCaptor.capture())
    }

    "be configured to use warn" in {
      val config = ConfigFactory.parseString(
        """
          |log-level=WARN
          |formatting {
          | this=that
          |}
        """.stripMargin)

      val underTest = LoggingSpanHandler(mockLogger, config)
      underTest.handle(testSpanInfo)

      verify(mockLogger).warn(logEntryCaptor.capture())
    }

    "be configured to use info" in {
      val config = ConfigFactory.parseString(
        """
          |log-level=INFO
          |formatting {
          | this=that
          |}
        """.stripMargin)
      val underTest = LoggingSpanHandler(mockLogger, config)
      underTest.handle(testSpanInfo)

      verify(mockLogger).info(logEntryCaptor.capture())
    }

    "be configured to use debug" in {
      val config = ConfigFactory.parseString(
        """
          |log-level=DEBUG
          |formatting {
          | this=that
          |}
        """.stripMargin)
      val underTest = LoggingSpanHandler(mockLogger, config)
      underTest.handle(testSpanInfo)

      verify(mockLogger).debug(logEntryCaptor.capture())
    }

    "be configured to use trace" in {
      val config = ConfigFactory.parseString(
        """
          |log-level=TRACE
          |formatting {
          | this=that
          |}
        """.stripMargin)
      val underTest = LoggingSpanHandler(mockLogger, config)
      underTest.handle(testSpanInfo)

      verify(mockLogger).trace(logEntryCaptor.capture())
    }

    "be configured to default to info" in {
      val config = ConfigFactory.parseString(
        """
          |formatting {
          | this=that
          |}
        """.stripMargin)
      val underTest = LoggingSpanHandler(mockLogger, config)
      underTest.handle(testSpanInfo)

      verify(mockLogger).info(logEntryCaptor.capture())
    }
  }
}
