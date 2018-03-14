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

package com.comcast.money.aspectj

import com.comcast.money.annotations.{ Timed, Traced }
import com.comcast.money.api.Span
import com.comcast.money.core._
import com.comcast.money.core.async.AsyncTracingService
import com.comcast.money.core.internal.{ MDCSupport, SpanLocal }
import com.comcast.money.core.logging.TraceLogging
import com.comcast.money.core.reflect.Reflections
import org.aspectj.lang.annotation.{ Around, Aspect, Pointcut }
import org.aspectj.lang.reflect.MethodSignature
import org.aspectj.lang.{ JoinPoint, ProceedingJoinPoint }
import org.slf4j.MDC

@Aspect
class TraceAspect extends Reflections with TraceLogging {

  val tracer: Tracer = Money.Environment.tracer
  val mdcSupport: MDCSupport = new MDCSupport()

  @Pointcut("execution(@com.comcast.money.annotations.Traced * *(..)) && @annotation(traceAnnotation)")
  def traced(traceAnnotation: Traced) = {}

  @Pointcut("execution(@com.comcast.money.annotations.Timed * *(..)) && @annotation(timedAnnotation)")
  def timed(timedAnnotation: Timed) = {}

  @Around("traced(traceAnnotation)")
  def adviseMethodsWithTracing(joinPoint: ProceedingJoinPoint, traceAnnotation: Traced): AnyRef = {
    if (traceAnnotation.async()) {
      traceMethodAsync(joinPoint, traceAnnotation)
    } else {
      traceMethod(joinPoint, traceAnnotation)
    }
  }

  private def traceMethod(joinPoint: ProceedingJoinPoint, traceAnnotation: Traced): AnyRef = {
    val key = traceAnnotation.value()
    val oldSpanName = mdcSupport.getSpanNameMDC
    var result = true

    try {
      tracer.startSpan(key)
      mdcSupport.setSpanNameMDC(Some(key))
      traceMethodArguments(joinPoint)

      joinPoint.proceed()
    } catch {
      case t: Throwable =>
        result = exceptionMatches(t, traceAnnotation.ignoredExceptions())
        logException(t)
        throw t
    } finally {
      tracer.stopSpan(result)
      mdcSupport.setSpanNameMDC(oldSpanName)
    }
  }

  private def traceMethodAsync(joinPoint: ProceedingJoinPoint, traceAnnotation: Traced): AnyRef = {
    val key = traceAnnotation.value()
    val asyncKey = key + "-async"
    val oldSpanName = mdcSupport.getSpanNameMDC
    var result = true
    var asyncSpan: Option[Span] = None

    try {
      tracer.startSpan(key)
      mdcSupport.setSpanNameMDC(Some(key))
      traceMethodArguments(joinPoint)

      tracer.startSpan(key + "-async")
      asyncSpan = SpanLocal.pop()

      val future = joinPoint.proceed()

      AsyncTracingService.findTracingService(future) match {
        case Some(service) =>
          service.whenDone(future, (_, t) => {
            val asyncResult = t == null || exceptionMatches(t, traceAnnotation.ignoredExceptions())
            logException(t)
            asyncSpan.foreach(_.stop(asyncResult))
          })
        case _ =>
          future
      }
    } catch {
      case t: Throwable =>
        result = exceptionMatches(t, traceAnnotation.ignoredExceptions())
        logException(t)
        throw t
    } finally {
      tracer.stopSpan(result)
      mdcSupport.setSpanNameMDC(oldSpanName)
    }
  }

  @Around("timed(timedAnnotation)")
  def adviseMethodsWithTiming(joinPoint: ProceedingJoinPoint, timedAnnotation: Timed): AnyRef = {
    val key: String = timedAnnotation.value
    val startTime = System.currentTimeMillis()
    try {
      joinPoint.proceed
    } finally {
      tracer.record(key, System.currentTimeMillis() - startTime)
    }
  }

  private def traceMethodArguments(joinPoint: JoinPoint): Unit = {
    if (joinPoint.getArgs != null && joinPoint.getArgs.length > 0) {
      joinPoint.getStaticPart.getSignature match {
        case signature: MethodSignature if signature.getMethod.getAnnotations != null =>
          recordTracedParameters(signature.getMethod, joinPoint.getArgs, tracer)
      }
    }
  }
}
