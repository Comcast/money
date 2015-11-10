package com.comcast.money.logging

import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{OneInstancePerTest, Matchers, WordSpec}
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
