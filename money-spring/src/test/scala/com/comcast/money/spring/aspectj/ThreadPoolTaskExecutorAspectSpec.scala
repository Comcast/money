package com.comcast.money.spring.aspectj

import java.util.concurrent.SynchronousQueue

import com.comcast.money.aspectj.ThreadPoolTaskExecutorAspect
import com.comcast.money.concurrent.TraceFriendlyThreadPoolExecutor
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
      executor.getThreadPoolExecutor.asInstanceOf[TraceFriendlyThreadPoolExecutor].allowsCoreThreadTimeOut() shouldBe true
    }

    "getField stops at object" in {
      val aspect = new ThreadPoolTaskExecutorAspect()
      aspect.getField(classOf[Object], "foo").isEmpty shouldBe true
    }

    "getFieldValue returns 0 for queueCapacity default" in {
      val aspect = new ThreadPoolTaskExecutorAspect()
      val executor = new ThreadPoolTaskExecutor
      executor.setQueueCapacity(100)
      val value : Int = aspect.getFieldValue(executor, "queueCapacity")
      value shouldBe 100
    }
  }
}
