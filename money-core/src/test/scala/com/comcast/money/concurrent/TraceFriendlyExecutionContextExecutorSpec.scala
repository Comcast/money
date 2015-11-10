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

package com.comcast.money.concurrent

import com.comcast.money.core.SpanId
import com.comcast.money.internal.SpanLocal
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ BeforeAndAfterEach, Matchers, OneInstancePerTest, WordSpec }
import org.slf4j.MDC

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }

class TraceFriendlyExecutionContextExecutorSpec extends WordSpec
    with Matchers
    with MockitoSugar
    with OneInstancePerTest
    with ConcurrentSupport
    with BeforeAndAfterEach {

  override def beforeEach() = {
    SpanLocal.clear()
    MDC.clear()
  }

  // brings in the implicit executor

  import com.comcast.money.concurrent.TraceFriendlyExecutionContextExecutor.Implicits.global

  "TraceFriendlyExecutionContext" should {
    "propagate the current trace local value" in {
      val originalSpanId = SpanId("1", 2L, 3L)
      SpanLocal.push(originalSpanId)

      val future = Future {
        SpanLocal.current
      }

      val futureResult = Await.result(future, 100 millis)
      futureResult shouldEqual Some(originalSpanId)
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
      val spanId1 = SpanId()
      val spanId2 = SpanId()
      SpanLocal.push(spanId1)
      SpanLocal.push(spanId2)

      val future = Future {
        SpanLocal.current
      }

      val futureResult = Await.result(future, 100 millis)
      futureResult shouldEqual Some(spanId2)
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
