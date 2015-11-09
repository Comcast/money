package com.comcast.money.aspectj

import java.lang.reflect.Field
import java.util.concurrent._

import com.comcast.money.concurrent.TraceFriendlyThreadPoolExecutor
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.{Aspect, Around, Pointcut}
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

import scala.util.Try

@Aspect
class ThreadPoolTaskExecutorAspect {
  def getField(clazz: Class[_], fieldName: String): Option[Field] = {

    if (clazz == classOf[Object])
      None
    else
      Try(clazz.getDeclaredField(fieldName)).toOption.orElse(getField(clazz.getSuperclass, fieldName))
  }

  def getFieldValue[T](instance:AnyRef, fieldName: String) :T = {
    getField(instance.getClass, fieldName).map( f => {
      f.setAccessible(true)
      f.get(instance).asInstanceOf[T]
    }).getOrElse(null.asInstanceOf[T])
  }

  def setFieldValue[T](instance:AnyRef, fieldName: String, value: T) = {
    getField(instance.getClass, fieldName).map( f => {
      f.setAccessible(true)
      f.set(instance, value)
    })
  }

  @Pointcut("execution(* org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor+.initializeExecutor(java.util.concurrent.ThreadFactory, java.util.concurrent.RejectedExecutionHandler)) && args(threadFactory, rejectedExecutionHandler)")
  def initializeExecutor(threadFactory: ThreadFactory, rejectedExecutionHandler: RejectedExecutionHandler) = { }

  @Around("initializeExecutor(threadFactory, rejectedExecutionHandler)")
  def aroundInitializeExector(joinPoint: ProceedingJoinPoint, threadFactory: ThreadFactory, rejectedExecutionHandler: RejectedExecutionHandler) : AnyRef = {
    val self = joinPoint.getThis.asInstanceOf[ThreadPoolTaskExecutor]

    val queueCapacity:Int = getFieldValue(self,"queueCapacity")
    val corePoolSize:Int = getFieldValue(self,"corePoolSize")
    val maxPoolSize:Int = getFieldValue(self,"maxPoolSize")
    val keepAliveSeconds:Int = getFieldValue(self,"keepAliveSeconds")
    val allowCoreThreadTimeOut:Boolean = getFieldValue(self,"allowCoreThreadTimeOut")

    val queue: BlockingQueue[Runnable] =
      if (queueCapacity > 0) {
        new LinkedBlockingQueue[Runnable](queueCapacity)
      }
      else {
        new SynchronousQueue[Runnable]
      }

    val executor: ThreadPoolExecutor = new TraceFriendlyThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveSeconds, TimeUnit.SECONDS, queue, threadFactory, rejectedExecutionHandler)
    if (allowCoreThreadTimeOut) {
      executor.allowCoreThreadTimeOut(true)
    }

    setFieldValue(self, "threadPoolExecutor", executor)

    executor
  }
}
