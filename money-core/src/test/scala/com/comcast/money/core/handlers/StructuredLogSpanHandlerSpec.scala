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

import com.typesafe.config.ConfigFactory
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito._
import org.slf4j.Logger
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.OneInstancePerTest
import org.slf4j.spi.MDCAdapter

class StructuredLogSpanHandlerSpec extends AnyWordSpec
  with Matchers with MockitoSugar with OneInstancePerTest with TestData {

  val mockLogger = mock[Logger]
  val mockLogFunction = mock[String => Unit]
  val mockMdcAdapter = mock[MDCAdapter]
  val sampleMessage = "sample formatted log message"

  val logEntryCaptor = ArgumentCaptor.forClass(classOf[String])

  val expectedLogValue = "[trace-id:5092ddfe-3701-4f84-b3d2-21f5501c0d28][parent-id:5176425846116696835][span-id:5176425846116696835][span-name:test-span][app:test][host:localhost][start-time:1970-01-01T00:00:00.100Z][end-time:1970-01-01T00:00:00.300Z][span-duration:200][span-success:true][str:bar][lng:200][dbl:1.2][bool:true]"
  val expectedLogValueWithHex = "[trace-id:5092ddfe37014f84b3d221f5501c0d28][parent-id:47d65be193efc303][span-id:47d65be193efc303][span-name:test-span][app:test][host:localhost][start-time:1970-01-01T00:00:00.100Z][end-time:1970-01-01T00:00:00.300Z][span-duration:200][span-success:true][str:bar][lng:200][dbl:1.2][bool:true]"

  "StructuredLogSpanHandler" should {
    "log span info" in {

      doAnswer({
        invocation =>
          val k = invocation.getArgument[String](0)
          val v = invocation.getArgument[String](1)
          k match {
            case "trace-id" => assert(v == "5092ddfe-3701-4f84-b3d2-21f5501c0d28")
            case "parent-id" => assert(v == "5176425846116696835")
            case "span-id" => assert(v == "5176425846116696835")
            case "span-name" => assert(v == "test-span")
            case "start-time" => assert(v == "1970-01-01T00:00:00.100Z")
            case "end-time" => assert(v == "1970-01-01T00:00:00.300Z")
            case "span-duration" => assert(v == "200")
            case "span-success" => assert(v == "true")
            case "name" => assert(v == "test-span")
            case "app" => assert(v == "test")
            case "host" => assert(v == "localhost")
            case "str" => assert(v == "bar")
            case "lng" => assert(v == "200")
            case "dbl" => assert(v == "1.2")
            case "bool" => assert(v == "true")
            case k => assert(false, s"Unknown property $k:$v added to MDC")
          }
      }).when(mockMdcAdapter).put(anyString(), anyString())

      val handler = new StructuredLogSpanHandler(mockLogFunction, mockMdcAdapter, false)

      handler.handle(fixedTestSpanInfo)

      verify(mockLogFunction)(expectedLogValue)

    }

    "log span info configured to log ids as hex" in {

      doAnswer({
        invocation =>
          val k = invocation.getArgument[String](0)
          val v = invocation.getArgument[String](1)
          k match {
            case "trace-id" => assert(v == "5092ddfe37014f84b3d221f5501c0d28")
            case "parent-id" => assert(v == "47d65be193efc303")
            case "span-id" => assert(v == "47d65be193efc303")
            case "span-name" => assert(v == "test-span")
            case "start-time" => assert(v == "1970-01-01T00:00:00.100Z")
            case "end-time" => assert(v == "1970-01-01T00:00:00.300Z")
            case "span-duration" => assert(v == "200")
            case "span-success" => assert(v == "true")
            case "name" => assert(v == "test-span")
            case "app" => assert(v == "test")
            case "host" => assert(v == "localhost")
            case "str" => assert(v == "bar")
            case "lng" => assert(v == "200")
            case "dbl" => assert(v == "1.2")
            case "bool" => assert(v == "true")
            case k => assert(false, s"Unknown property $k:$v added to MDC")
          }
      }).when(mockMdcAdapter).put(anyString(), anyString())

      val handler = new StructuredLogSpanHandler(mockLogFunction, mockMdcAdapter, true)

      handler.handle(fixedTestSpanInfo)

      verify(mockLogFunction)(expectedLogValueWithHex)

    }

    "be configured to use error" in {
      val config = ConfigFactory.parseString(
        """
          |log-level=ERROR
          |formatting {
          | this=that
          |}
        """.stripMargin)

      val underTest = StructuredLogSpanHandler(mockLogger, config)
      underTest.handle(fixedTestSpanInfo)

      verify(mockLogger).error(expectedLogValue)
    }

    "be configured to use warn" in {
      val config = ConfigFactory.parseString(
        """
          |log-level=WARN
          |formatting {
          | this=that
          |}
        """.stripMargin)

      val underTest = StructuredLogSpanHandler(mockLogger, config)
      underTest.handle(fixedTestSpanInfo)

      verify(mockLogger).warn(expectedLogValue)
    }

    "be configured to use info" in {
      val config = ConfigFactory.parseString(
        """
          |log-level=INFO
          |formatting {
          | this=that
          |}
        """.stripMargin)

      val underTest = StructuredLogSpanHandler(mockLogger, config)
      underTest.handle(fixedTestSpanInfo)

      verify(mockLogger).info(expectedLogValue)
    }

    "be configured to use debug" in {
      val config = ConfigFactory.parseString(
        """
          |log-level=DEBUG
          |formatting {
          | this=that
          |}
        """.stripMargin)

      val underTest = StructuredLogSpanHandler(mockLogger, config)
      underTest.handle(fixedTestSpanInfo)

      verify(mockLogger).debug(expectedLogValue)
    }

    "be configured to use trace" in {
      val config = ConfigFactory.parseString(
        """
          |log-level=TRACE
          |formatting {
          | this=that
          |}
        """.stripMargin)

      val underTest = StructuredLogSpanHandler(mockLogger, config)
      underTest.handle(fixedTestSpanInfo)

      verify(mockLogger).trace(expectedLogValue)
    }

    "be configured to default to info" in {
      val config = ConfigFactory.parseString(
        """
          |formatting {
          | this=that
          |}
        """.stripMargin)

      val underTest = StructuredLogSpanHandler(mockLogger, config)
      underTest.handle(fixedTestSpanInfo)

      verify(mockLogger).info(expectedLogValue)
    }
  }
}
