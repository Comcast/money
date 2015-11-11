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
import com.comcast.money.internal.MDCSupport
import com.comcast.money.logging.TraceLogging
import com.comcast.money.reflect.Reflections
import org.aspectj.lang.annotation.{ Around, Aspect, Pointcut }
import org.aspectj.lang.reflect.MethodSignature
import org.aspectj.lang.{ JoinPoint, ProceedingJoinPoint }

@Aspect
class TraceAspect extends Reflections with TraceLogging {

  val tracer: Tracer = Money.tracer
  val mdcSupport: MDCSupport = new MDCSupport()

  @Pointcut("execution(@com.comcast.money.annotations.Traced * *(..)) && @annotation(traceAnnotation)")
  def traced(traceAnnotation: Traced) = {}

  @Pointcut("execution(@com.comcast.money.annotations.Timed * *(..)) && @annotation(timedAnnotation)")
  def timed(timedAnnotation: Timed) = {}

  @Around("traced(traceAnnotation)")
  def adviseMethodsWithTracing(joinPoint: ProceedingJoinPoint, traceAnnotation: Traced): AnyRef = {
    val key: String = traceAnnotation.value
    var result = Result.success
    val oldSpanName = mdcSupport.getSpanNameMDC
    try {
      tracer.startSpan(key)
      mdcSupport.setSpanNameMDC(Some(key))
      traceMethodArguments(joinPoint)
      joinPoint.proceed
    } catch {
      case t: Throwable =>
        result = if (exceptionMatches(t, traceAnnotation.ignoredExceptions())) Result.success else Result.failed
        logException(t)
        throw t
    } finally {
      mdcSupport.setSpanNameMDC(oldSpanName)
      tracer.stopSpan(result)
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
