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

package com.comcast.money.core.concurrent

import com.comcast.money.api.SpanId
import com.comcast.money.core.SpecHelpers
import com.comcast.money.core.internal.SpanLocal
import org.mockito.Mockito._
import org.slf4j.MDC

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{ OneInstancePerTest, BeforeAndAfterEach }

class TraceFriendlyExecutionContextExecutorSpec extends AnyWordSpec
  with Matchers
  with MockitoSugar
  with OneInstancePerTest
  with ConcurrentSupport
  with SpecHelpers
  with BeforeAndAfterEach {

  import com.comcast.money.core.concurrent.TraceFriendlyExecutionContextExecutor.Implicits.global

  override def beforeEach() = {
    SpanLocal.clear()
    MDC.clear()
  }

  // brings in the implicit executor

  "TraceFriendlyExecutionContext" should {
    "propagate the current trace local value" in {
      val originalSpanId = new SpanId("1", 2L, 3L)
      val originalSpan = testSpan(originalSpanId)
      SpanLocal.push(originalSpan)

      val future = Future {
        SpanLocal.current.get.info.id
      }

      val futureResult = Await.result(future, 100 millis)
      futureResult shouldEqual originalSpanId
    }
    "propagate no span value if none is present" in {
      SpanLocal.clear()

      val future = Future {
        SpanLocal.current
      }

      val futureResult = Await.result(future, 100 millis)
      futureResult shouldEqual None
    }
    "propagate only the latest span id value" in {
      val spanId1 = new SpanId()
      val spanId2 = new SpanId()
      SpanLocal.push(testSpan(spanId1))
      SpanLocal.push(testSpan(spanId2))

      val future = Future {
        SpanLocal.current.get.info.id
      }

      val futureResult = Await.result(future, 100 millis)
      futureResult shouldEqual spanId2
    }
    "delegate reportFailure to the wrapped executor" in {
      val mockExecutionContext = mock[ExecutionContext]
      val traceFriendly = TraceFriendlyExecutionContextExecutor(mockExecutionContext)
      val failure = new IllegalArgumentException()

      traceFriendly.reportFailure(failure)
      verify(mockExecutionContext).reportFailure(failure)
    }
    "propogate MDC data" in {
      MDC.put("FINGERPRINT", "print")
      val future = Future {
        MDC.get("FINGERPRINT")
      }
      MDC.get("FINGERPRINT") shouldEqual "print"
      Await.result(future, 100 millis) shouldEqual "print"
    }

    "Child MDC should not escape to parent " in {
      val future = Future {
        MDC.put("FINGERPRINT", "print")
        MDC.get("FINGERPRINT")
      }
      MDC.get("FINGERPRINT") shouldBe null
      Await.result(future, 100 millis) shouldEqual "print"
    }
  }
}
