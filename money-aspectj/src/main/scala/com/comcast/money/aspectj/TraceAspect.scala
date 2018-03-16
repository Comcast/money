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
import com.comcast.money.core._
import com.comcast.money.core.async.AsyncNotificationService._
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
    val key = traceAnnotation.value()
    val oldSpanName = mdcSupport.getSpanNameMDC
    var result = true
    var stopSpan = true

    try {
      tracer.startSpan(key)
      mdcSupport.setSpanNameMDC(Some(key))
      traceMethodArguments(joinPoint)

      val returnValue = joinPoint.proceed()

      if (traceAnnotation.async()) {
        findNotificationService(returnValue).map(service => {
          stopSpan = false
          val span = SpanLocal.pop()
          val mdc = Option(MDC.getCopyOfContextMap)

          service.whenDone(returnValue, (_, t) => {
            mdcSupport.propogateMDC(mdc)
            val asyncResult = t == null || exceptionMatches(t, traceAnnotation.ignoredExceptions())
            logException(t)
            span.foreach(_.stop(asyncResult))
          })
        }).getOrElse(returnValue)
      } else {
        returnValue
      }
    } catch {
      case t: Throwable =>
        result = exceptionMatches(t, traceAnnotation.ignoredExceptions())
        logException(t)
        throw t
    } finally {
      if (stopSpan) {
        tracer.stopSpan(result)
      }
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
