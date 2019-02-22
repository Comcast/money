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

package com.comcast.money.aspectj

import com.comcast.money.annotations.{ Timed, Traced }
import com.comcast.money.core.internal.MethodTracer
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.{ Around, Aspect, Pointcut }
import org.aspectj.lang.reflect.MethodSignature

@Aspect
class TraceAspect extends MethodTracer {

  @Pointcut("execution(@com.comcast.money.annotations.Traced * *(..)) && @annotation(traceAnnotation)")
  def traced(traceAnnotation: Traced): Unit = {}

  @Pointcut("execution(@com.comcast.money.annotations.Timed * *(..)) && @annotation(timedAnnotation)")
  def timed(timedAnnotation: Timed): Unit = {}

  @Around("traced(traceAnnotation)")
  def adviseMethodsWithTracing(joinPoint: ProceedingJoinPoint, traceAnnotation: Traced): AnyRef = {
    joinPoint.getSignature match {
      case methodSignature: MethodSignature =>
        traceMethod(methodSignature.getMethod, traceAnnotation, joinPoint.getArgs, joinPoint.proceed)
      case _ => joinPoint.proceed()
    }
  }

  @Around("timed(timedAnnotation)")
  def adviseMethodsWithTiming(joinPoint: ProceedingJoinPoint, timedAnnotation: Timed): AnyRef = {
    joinPoint.getSignature match {
      case methodSignature: MethodSignature =>
        timeMethod(methodSignature.getMethod, timedAnnotation, joinPoint.proceed)
      case _ => joinPoint.proceed()
    }
  }
}
