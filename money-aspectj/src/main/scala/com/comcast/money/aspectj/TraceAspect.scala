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
import com.comcast.money.core._
import com.comcast.money.core.async.AsyncNotifier
import com.comcast.money.core.internal.{ MDCSupport, SpanLocal }
import com.comcast.money.core.logging.TraceLogging
import com.comcast.money.core.reflect.Reflections
import org.aspectj.lang.annotation.{ Around, Aspect, Pointcut }
import org.aspectj.lang.reflect.MethodSignature
import org.aspectj.lang.{ JoinPoint, ProceedingJoinPoint }
import org.slf4j.MDC

import scala.util.{ Failure, Success }

@Aspect
class TraceAspect extends Reflections with TraceLogging {

  val tracer: Tracer = Money.Environment.tracer
  val asyncNotifier: AsyncNotifier = Money.Environment.asyncNotifier
  val mdcSupport: MDCSupport = new MDCSupport()

  @Pointcut("execution(@com.comcast.money.annotations.Traced * *(..)) && @annotation(traceAnnotation)")
  def traced(traceAnnotation: Traced) = {}

  @Pointcut("execution(@com.comcast.money.annotations.Timed * *(..)) && @annotation(timedAnnotation)")
  def timed(timedAnnotation: Timed) = {}

  @Around("traced(traceAnnotation)")
  def adviseMethodsWithTracing(joinPoint: ProceedingJoinPoint, traceAnnotation: Traced): AnyRef = {
    val key = traceAnnotation.value()
    val oldSpanName = mdcSupport.getSpanNameMDC
    var spanResult: Option[Boolean] = Some(true)

    try {
      tracer.startSpan(key)
      mdcSupport.setSpanNameMDC(Some(key))
      traceMethodArguments(joinPoint)

      val returnValue = joinPoint.proceed()

      if (traceAnnotation.async()) {
        traceAsyncResult(traceAnnotation, returnValue) match {
          case Some(asyncResult) =>
            // Do not stop the span when the advice returns as the span will
            // be stopped by the callback registered to the `AsyncNotificationHandler`
            spanResult = None
            asyncResult
          case None =>
            returnValue
        }
      } else {
        returnValue
      }
    } catch {
      case t: Throwable =>
        spanResult = Some(exceptionMatches(t, traceAnnotation.ignoredExceptions()))
        logException(t)
        throw t
    } finally {
      spanResult.foreach(tracer.stopSpan)
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

  /**
   * Binds the duration and result of the current span to the return value of the traced method
   *
   * @param traceAnnotation The `@Traced` annotation applied to the method
   * @param returnValue The return value from the `@Traced` method
   * @return An option with the result from the `AsyncNotificationHandler`, or `None` if no handler
   *         supports the return value
   */
  private def traceAsyncResult(traceAnnotation: Traced, returnValue: AnyRef): Option[AnyRef] =
    // attempt to resolve the AsyncNotificationHandler for the return value
    asyncNotifier.resolveHandler(returnValue).map {
      handler =>
        // pop the current span from the stack as it will not be stopped by the tracer
        val span = SpanLocal.pop()
        // capture the current MDC context to be applied on the callback thread
        val mdc = Option(MDC.getCopyOfContextMap)

        // register callback to be invoked when the future is completed
        handler.whenComplete(returnValue, completed => {

          // reapply the MDC onto the callback thread
          mdcSupport.propogateMDC(mdc)

          // determine if the future completed successfully or exceptionally
          val result = completed match {
            case Success(_) => true
            case Failure(exception) =>
              logException(exception)
              exceptionMatches(exception, traceAnnotation.ignoredExceptions())
          }

          // stop the captured span with the success/failure flag
          span.foreach(_.stop(result))
          // clear the MDC from the callback thread
          MDC.clear()
        })
    }
}
