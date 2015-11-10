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

package com.comcast.money.core

import com.comcast.money.logging.TraceLogging

object Tracers extends TraceLogging {

  /**
   * Executes the function provided within a new trace span
   * @param name The name of the span, this will be what is emitted to logs and other listeners
   * @param tracer The tracer instance to use, if not specified defaults to the standard Money.tracer.  This is
   *               overridable primarily for testing purposes
   * @param f The function to be executed
   * @tparam T The return type of the function
   * @return The result of the function being executed
   */
  def traced[T](name: String, tracer: Tracer = Money.tracer)(f: => T): T = {
    try {
      tracer.startSpan(name)
      val result: T = f
      tracer.stopSpan(Result.success)
      result
    } catch {
      case e: Throwable =>
        logException(e)
        tracer.stopSpan(Result.failed)
        throw e
    }
  }

  /**
   * Times the execution of the function, appends the duration to the current trace
   * @param name The name of the timer
   * @param tracer The tracer instance to use, if not specified defaults to the standard Money.tracer.  This is
   *               overridableprimarily for testing purposes
   * @param f The function to be executed
   * @tparam T The return type of the function
   * @return The result of the function being executed
   */
  def timed[T](name: String, tracer: Tracer = Money.tracer)(f: => T): T = {
    try {
      tracer.startTimer(name)
      f
    } finally {
      tracer.stopTimer(name)
    }
  }
}
