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

package com.comcast.money.spring.aspectj

import java.util.concurrent.SynchronousQueue

import com.comcast.money.aspectj.ThreadPoolTaskExecutorAspect
import com.comcast.money.core.concurrent.TraceFriendlyThreadPoolExecutor
import org.scalatest._
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

class ThreadPoolTaskExecutorAspectSpec extends WordSpec with Matchers {

  class MyThreadPoolTaskExecutor extends ThreadPoolTaskExecutor

  "ThreadPoolTaskExecutorAspectSpec" should {
    "create a trace friendly executor" in {
      val executor = new MyThreadPoolTaskExecutor()
      executor.afterPropertiesSet()

      executor.getThreadPoolExecutor shouldBe a[TraceFriendlyThreadPoolExecutor]
    }

    "use a synchronous queue when 0 capacity" in {
      val executor = new MyThreadPoolTaskExecutor()
      executor.setQueueCapacity(0)
      executor.afterPropertiesSet()

      executor.getThreadPoolExecutor shouldBe a[TraceFriendlyThreadPoolExecutor]
      executor.getThreadPoolExecutor.getQueue shouldBe a[SynchronousQueue[_]]
    }

    "set allow core thread timeout" in {
      val executor = new MyThreadPoolTaskExecutor()
      executor.setAllowCoreThreadTimeOut(true)
      executor.afterPropertiesSet()

      executor.getThreadPoolExecutor shouldBe a[TraceFriendlyThreadPoolExecutor]
      executor.getThreadPoolExecutor.asInstanceOf[TraceFriendlyThreadPoolExecutor]
        .allowsCoreThreadTimeOut() shouldBe true
    }

    "getField stops at object" in {
      val aspect = new ThreadPoolTaskExecutorAspect()
      aspect.getField(classOf[Object], "foo").isEmpty shouldBe true
    }

    "getFieldValue returns 0 for queueCapacity default" in {
      val aspect = new ThreadPoolTaskExecutorAspect()
      val executor = new ThreadPoolTaskExecutor
      executor.setQueueCapacity(100)
      val value: Int = aspect.getFieldValue(executor, "queueCapacity")
      value shouldBe 100
    }
  }
}
