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

package com.comcast.money.core.logging

import java.lang.reflect.Method

import com.comcast.money.annotations.{ Timed, Traced }
import com.comcast.money.core.{ Money, Tracer }
import com.comcast.money.core.async.AsyncNotifier
import com.comcast.money.core.internal.{ MDCSupport, SpanLocal }
import com.comcast.money.core.reflect.Reflections
import org.slf4j.MDC

import scala.util.{ Failure, Success, Try }

trait MethodTracer extends Reflections with TraceLogging {
  val tracer: Tracer = Money.Environment.tracer
  val asyncNotifier: AsyncNotifier = Money.Environment.asyncNotifier
  val mdcSupport: MDCSupport = new MDCSupport()

  def traceMethod(method: Method, annotation: Traced, args: Array[AnyRef], proceed: () => AnyRef): AnyRef = {
    val key = annotation.value()

    tracer.startSpan(key)
    recordTracedParameters(method, args, tracer)

    Try { proceed() } match {
      case Success(result) if annotation.async() =>
        traceAsyncResult(method, annotation, result) match {
          case Some(future) =>
            future
          case None =>
            tracer.stopSpan(true)
            result
        }
      case Success(result) =>
        tracer.stopSpan(true)
        result
      case Failure(exception) =>
        logException(exception)
        tracer.stopSpan(exceptionMatches(exception, annotation.ignoredExceptions()))
        throw exception
    }
  }

  def timeMethod(method: Method, annotation: Timed, proceed: () => AnyRef): AnyRef = {
    val key = annotation.value()
    try {
      tracer.startTimer(key)
      proceed()
    } finally {
      tracer.startTimer(key)
    }
  }

  def traceAsyncResult(
    method: Method,
    annotation: Traced,
    returnValue: AnyRef): Option[AnyRef] = for {

    // resolve an async notification handler that supports the result
    handler <- asyncNotifier.resolveHandler(method.getReturnType, returnValue)

    // pop the current span from the stack as it will not be stopped by the tracer
    span <- SpanLocal.pop()
    // capture the current MDC context to be applied on the callback thread
    mdc = Option(MDC.getCopyOfContextMap)

    result = handler.whenComplete(method.getReturnType, returnValue) { completed =>
      // reapply the MDC onto the callback thread
      mdcSupport.propogateMDC(mdc)

      // determine if the future completed successfully or exceptionally
      val result = completed match {
        case Success(_) => true
        case Failure(exception) =>
          logException(exception)
          exceptionMatches(exception, annotation.ignoredExceptions())
      }

      // stop the captured span with the success/failure flag
      span.stop(result)
      // clear the MDC from the callback thread
      MDC.clear()
    }
  } yield result
}
