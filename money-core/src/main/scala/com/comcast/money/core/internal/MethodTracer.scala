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

package com.comcast.money.core.internal

import java.lang.reflect.Method

import com.comcast.money.annotations.{ Timed, Traced }
import com.comcast.money.core.async.AsyncNotifier
import com.comcast.money.core.logging.TraceLogging
import com.comcast.money.core.reflect.Reflections
import com.comcast.money.core.{ Money, Tracer }
import org.slf4j.MDC

import scala.util.control.ControlThrowable
import scala.util.{ Failure, Success, Try }

/**
 * Helper trait for tracing method invocations by aspect.
 */
trait MethodTracer extends Reflections with TraceLogging {
  val tracer: Tracer = Money.Environment.tracer
  val asyncNotifier: AsyncNotifier = Money.Environment.asyncNotifier
  val mdcSupport: MDCSupport = new MDCSupport()

  /**
   * Wraps the invocation of the method so that it can be traced.
   *
   * @param method the method being invoked
   * @param annotation the `Traced` annotation applied to the method
   * @param args the actual arguments being passed to the method
   * @param proceed the function to proceed invocation of the method
   * @return the return value of the traced method
   */
  def traceMethod(method: Method, annotation: Traced, args: Array[AnyRef], proceed: () => AnyRef): AnyRef = {
    val key = annotation.value()

    tracer.startSpan(key)
    recordTracedParameters(method, args, tracer)

    invoke(proceed) match {
      case Success(result) =>
        traceAsynchronousInvocation(method, annotation, result) match {
          case Some(future) =>
            future
          case None =>
            tracer.stopSpan(true)
            result
        }
      case Failure(exception) =>
        logException(exception)
        tracer.stopSpan(exceptionMatches(exception, annotation.ignoredExceptions()))
        throw exception
    }
  }

  /**
   * Invokes the join point wrapping in a `Try` while handling additional `Fatal` exceptions
   * that we'd still want to correctly stop the `Span`
   */
  private def invoke(proceed: () => AnyRef): Try[AnyRef] = {
    try Try(proceed())
    catch {
      case exception: ControlThrowable =>
        Failure(exception)
      case exception: InterruptedException =>
        Failure(exception)
    }
  }

  /**
   * Wraps the invocation of the method so that it can be timed.
   * @param method the method being invoked
   * @param annotation the `Timed` annotation applied to the method
   * @param proceed the function to proceed invocation of the method
   * @return the return value of the timed method
   */
  def timeMethod(method: Method, annotation: Timed, proceed: () => AnyRef): AnyRef = {
    val key = annotation.value()
    try {
      tracer.startTimer(key)
      proceed()
    } finally {
      tracer.startTimer(key)
    }
  }

  /**
   * Attempts to asynchronously trace the result of the method call based on the return type
   * and the return value.  The asynchronous tracing is only enabled when `async` is set to `true`
   * in the `Traced` annotation applied to the method
   *
   * @param method the method that was called
   * @param annotation the `Traced` annotation
   * @param returnValue the return value of the method
   * @return `Some` of the future if the return value could be traced asynchronously; otherwise, `None`
   */
  def traceAsynchronousInvocation(
    method: Method,
    annotation: Traced,
    returnValue: AnyRef): Option[AnyRef] = for {

    // get the return type of the traced method
    returnType <- Option(method.getReturnType)

    // only attempt asynchronous tracing if the traced method is marked as asynchronous
    if annotation.async()

    // attempt to resolve a handler that can register for the completion of the future
    handler <- asyncNotifier.resolveHandler(returnType, returnValue)

    // pop the current span from the stack as it will not be stopped by the tracer
    span <- SpanLocal.pop()
    // capture the current MDC context to be applied on the callback thread
    mdc = Option(MDC.getCopyOfContextMap)

    // register for completion of the future
    future = handler.whenComplete(returnType, returnValue) { completed =>
      // reapply the MDC onto the callback thread
      mdcSupport.propogateMDC(mdc)

      // determine if the future completed successfully or exceptionally
      val success = completed match {
        case Success(_) => true
        case Failure(exception) =>
          logException(exception)
          exceptionMatches(exception, annotation.ignoredExceptions())
      }

      // stop the captured span with the success/failure flag
      span.stop(success)
      // clear the MDC from the callback thread
      MDC.clear()
    }
  } yield future
}
