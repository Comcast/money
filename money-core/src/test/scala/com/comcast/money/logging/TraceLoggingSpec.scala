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

package com.comcast.money.logging

import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ OneInstancePerTest, Matchers, WordSpec }
import org.slf4j.Logger

class TraceLoggingSpec extends WordSpec with Matchers with MockitoSugar with OneInstancePerTest {

  val mockLogger = mock[Logger]

  "TraceLogging" should {
    "capture exceptions into a log" in {
      val testTraceLogging = new TraceLogging {
        override lazy val shouldLogExceptions: Boolean = true
        override val logger: Logger = mockLogger
      }

      val t = mock[Throwable]
      testTraceLogging.logException(t)
      verify(mockLogger).error("Tracing exception", t)
    }
    "not capture exceptions if log exceptions is not enabled" in {
      val testTraceLogging = new TraceLogging {
        override lazy val shouldLogExceptions: Boolean = false
        override val logger: Logger = mockLogger
      }
      val t = mock[Throwable]
      testTraceLogging.logException(t)
      verifyZeroInteractions(mockLogger)
    }
  }
}
