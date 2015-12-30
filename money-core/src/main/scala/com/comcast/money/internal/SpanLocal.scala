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

package com.comcast.money.internal

import com.comcast.money.api.SpanId

import scala.collection._

/**
 * Provides a thread local context for storing SpanIds.  Keeps a stack of trace ids so that we
 * an roll back to the parent once a span completes
 */
object SpanLocal {
  type SpanStack = mutable.Stack[SpanId]

  // A stack of span ids for the current thread
  private[this] val threadLocalCtx = new ThreadLocal[SpanStack]

  private lazy val mdcSupport = new MDCSupport()

  import mdcSupport._

  /**
   * @return Some([[com.comcast.money.core.SpanIdHttpFormatter]] that is the current span id for this thread; or [[scala.None]] if
   *         there is no span for this thread
   */
  def current: Option[SpanId] = {
    Option(threadLocalCtx.get).flatMap {
      stack => stack.headOption
    }
  }

  /**
   * Appends the [[com.comcast.money.core.SpanIdHttpFormatter]] provided to the thread's stack.  The new SpanId is considered
   * "active", and will also
   * be the new current SpanId used in future span requests
   *
   * @param spanId The [[com.comcast.money.core.SpanIdHttpFormatter]] that will be the new span
   */
  def push(spanId: SpanId): Unit = {
    if (spanId != null) {
      Option(threadLocalCtx.get) match {
        case Some(stack) =>
          stack.push(spanId)
          setSpanMDC(Some(spanId))
        case None =>
          threadLocalCtx.set(new SpanStack())
          push(spanId)
      }
    }
  }

  /**
   * Removes the span id from the current thread.  If there were previous span ids (parent span ids),
   * the current span id will be set to the parent span id
   * @return Some[[com.comcast.money.core.SpanIdHttpFormatter]] containing the span id that was removed from the thread; or
   *         [[scala.None]] if there is no
   *         current [[com.comcast.money.core.SpanIdHttpFormatter]] for the thread
   */
  def pop(): Option[SpanId] = {
    Option(threadLocalCtx.get).map {
      stack =>
        val spanId = stack.pop()
        setSpanMDC(stack.headOption)
        spanId
    }
  }

  /**
   * Clears the entire call stack for the thread
   */
  def clear() = {
    threadLocalCtx.remove()
    setSpanMDC(None)
  }
}
