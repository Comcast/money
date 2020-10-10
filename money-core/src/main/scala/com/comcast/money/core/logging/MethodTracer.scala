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
import com.comcast.money.api.Span
import com.comcast.money.core.{ Money, Tracer }
import com.comcast.money.core.async.AsyncNotifier
import com.comcast.money.core.internal.Resources.withResource
import com.comcast.money.core.internal.{ MDCSupport, SpanContext, SpanLocal }
import com.comcast.money.core.reflect.Reflections
import org.slf4j.MDC

import scala.util.{ Failure, Success, Try }

trait MethodTracer extends Reflections with TraceLogging {
  val tracer: Tracer = Money.Environment.tracer
  val asyncNotifier: AsyncNotifier = Money.Environment.asyncNotifier
  val mdcSupport: MDCSupport = new MDCSupport()
  val spanContext: SpanContext = SpanLocal

  def traceMethod(method: Method, annotation: Traced, args: Array[AnyRef], proceed: () => AnyRef): AnyRef = {
    val key = annotation.value()

    val span = tracer.spanBuilder(key).startSpan()
    val scope = tracer.withSpan(span)

    try {
      recordTracedParameters(method, args, tracer)

      Try {
        proceed()
      } match {
        case Success(result) if annotation.async() =>
          traceAsyncResult(method, annotation, span, result) match {
            case Some(future) =>
              future
            case None =>
              span.stop(true)
              result
          }
        case Success(result) =>
          span.stop(true)
          result
        case Failure(exception) =>
          logException(exception)
          span.stop(exceptionMatches(exception, annotation.ignoredExceptions()))
          throw exception
      }
    } finally {
      scope.close()
    }
  }

  def timeMethod(method: Method, annotation: Timed, proceed: () => AnyRef): AnyRef = {
    val key = annotation.value()
    val scope = tracer.startTimer(key)
    try {
      proceed()
    } finally {
      scope.close()
    }
  }

  def traceAsyncResult(
    method: Method,
    annotation: Traced,
    span: Span,
    returnValue: AnyRef): Option[AnyRef] = for {

    // resolve an async notification handler that supports the result
    handler <- asyncNotifier.resolveHandler(method.getReturnType, returnValue)

    // capture the current MDC context to be applied on the callback thread
    mdc = Option(MDC.getCopyOfContextMap)

    result = handler.whenComplete(method.getReturnType, returnValue) { completed =>
      // reapply the MDC onto the callback thread
      mdcSupport.propagateMDC(mdc)

      // apply the span onto the current thread context
      val scope = tracer.withSpan(span)

      try {
        // determine if the future completed successfully or exceptionally
        val result = completed match {
          case Success(_) => true
          case Failure(exception) =>
            logException(exception)
            exceptionMatches(exception, annotation.ignoredExceptions())
        }

        // stop the captured span with the success/failure flag
        span.stop(result)
      } finally {
        // reset the current thread context
        scope.close()

        // clear the MDC from the callback thread
        MDC.clear()
      }
    }
  } yield result
}
