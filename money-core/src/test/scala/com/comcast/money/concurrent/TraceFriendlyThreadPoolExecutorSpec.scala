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

import java.util.concurrent.{ Callable, ExecutorService }

import com.comcast.money.api.SpanId
import com.comcast.money.core.SpanId
import com.comcast.money.internal.SpanLocal
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ Matchers, OneInstancePerTest, WordSpecLike }
import org.slf4j.MDC

class TraceFriendlyThreadPoolExecutorSpec
    extends WordSpecLike with MockitoSugar with Matchers with ConcurrentSupport with OneInstancePerTest {

  val executor: ExecutorService = TraceFriendlyThreadPoolExecutor.newCachedThreadPool

  "TraceFriendlyThreadPoolExecutor cachedThreadPool" should {
    "propagate the current span local value" in {
      val traceId = new SpanId("1", 2L, 3L)
      SpanLocal.push(traceId)

      val future = executor.submit(testCallable)

      future.get shouldEqual Some(traceId)
      SpanLocal.clear()
    }
    "propagate no span value if none is present" in {
      SpanLocal.clear()

      val future = executor.submit(testCallable)

      future.get shouldEqual None
      SpanLocal.current shouldEqual None
    }
    "propagate only the current span id value" in {
      val traceId1 = new SpanId()
      val traceId2 = new SpanId()
      SpanLocal.push(traceId1)
      SpanLocal.push(traceId2)

      val future = executor.submit(testCallable)
      future.get shouldEqual Some(traceId2)
    }
    "propagate MDC" in {
      val traceId = new SpanId("1", 2L, 3L)
      SpanLocal.push(traceId)
      MDC.put("foo", "bar")

      val mdcCallable = new Callable[String] {
        override def call(): String = MDC.get("foo")
      }

      val future = executor.submit(mdcCallable)

      future.get shouldEqual "bar"
      SpanLocal.clear()
    }
  }
  "TraceFriendlyThreadPoolExecutor fixedThreadPool" should {
    val threadPool: TraceFriendlyThreadPoolExecutor = TraceFriendlyThreadPoolExecutor.newFixedThreadPool(1)
      .asInstanceOf[TraceFriendlyThreadPoolExecutor]

    "created the pool with the specified number of threads" in {
      threadPool.getCorePoolSize shouldEqual 1
    }
  }
}
