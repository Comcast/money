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

import com.typesafe.config.ConfigFactory
import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, OneInstancePerTest, WordSpec}
import org.slf4j.Logger

class StructuredLogSpanHandlerSpec extends WordSpec
    with Matchers with MockitoSugar with OneInstancePerTest with TestData {

  val mockLogger = mock[Logger]
  val sampleMessage = "sample formatted log message"

  val logEntryCaptor = ArgumentCaptor.forClass(classOf[String])
  val underTest = new StructuredLogSpanHandler(mockLogger)

  "StructuredLogSpanHandler" should {
    "log span info" in {

      val handler = new StructuredLogSpanHandler(mockLogger,
        (k,v)=>k match{
        case "trace-id" => assert(v=="5092ddfe-3701-4f84-b3d2-21f5501c0d28")
        case "parent-id" =>assert(v=="5176425846116696835")
        case "span-id" => assert(v=="5176425846116696835")
        case "span-name" => assert(v=="test-span")
        case "start-time" => assert(v=="100")
        case "end-time" => assert(v=="300")
        case "span-duration" => assert(v=="200")
        case "span-success" => assert(v=="true")
        case "name" => assert(v=="test-span")
        case "app" => assert(v=="test")
        case "host" => assert(v=="localhost")
        case "str" => assert(v=="bar")
        case "lng" => assert(v=="200")
        case "dbl" => assert(v=="1.2")
        case "bool" => assert(v=="true")
        case k => assert(false, s"Unknown property $k:$v added to MDC")
      })

      handler.handle(fixedTestSpanInfo)

      verify(mockLogger).info(logEntryCaptor.capture())
      assert(logEntryCaptor.getValue =="[trace-id:5092ddfe-3701-4f84-b3d2-21f5501c0d28][parent-id:5176425846116696835][span-id:5176425846116696835][span-name:test-span][app:test][host:localhost][start-time:100][end-time:300][span-duration:200][span-success:true][str:bar][lng:200][dbl:1.2][bool:true]")

     }


    "be configured to use error" in {
      val config = ConfigFactory.parseString(
        """
          |log-level=ERROR
          |formatting {
          | this=that
          |}
        """.stripMargin
      )

      underTest.configure(config)
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
        """.stripMargin
      )

      underTest.configure(config)
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
        """.stripMargin
      )

      underTest.configure(config)
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
        """.stripMargin
      )

      underTest.configure(config)
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
        """.stripMargin
      )

      underTest.configure(config)
      underTest.handle(testSpanInfo)

      verify(mockLogger).trace(logEntryCaptor.capture())
    }

    "be configured to default to info" in {
      val config = ConfigFactory.parseString(
        """
          |formatting {
          | this=that
          |}
        """.stripMargin
      )

      underTest.configure(config)
      underTest.handle(testSpanInfo)

      verify(mockLogger).info(logEntryCaptor.capture())
    }
  }
}
