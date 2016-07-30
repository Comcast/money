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

package com.comcast.money.core.internal

import com.comcast.money.api.Span

import scala.collection.mutable.Stack

trait SpanLocal {
  def current(): Option[Span]
  def push(span: Span): Unit
  def pop(): Option[Span]
  def clear(): Unit
}

trait ThreadLocalSpanTracer {
  val SpanLocal: SpanLocal = SpanThreadLocal
}

/**
 * Provides a thread local context for storing SpanIds.  Keeps a stack of trace ids so that we
 * an roll back to the parent once a span completes
 */
object SpanThreadLocal extends SpanLocal {

  // A stack of span ids for the current thread
  private[this] val threadLocalCtx = new ThreadLocal[Stack[Span]]

  private lazy val mdcSupport = new MDCSupport()

  import mdcSupport._

  override def current: Option[Span] = {
    Option(threadLocalCtx.get).flatMap(_.headOption)
  }

  override def push(span: Span): Unit = {
    if (span != null) {
      Option(threadLocalCtx.get) match {
        case Some(stack) =>
          stack.push(span)
          setSpanMDC(Some(span.info.id))
        case None =>
          threadLocalCtx.set(new Stack[Span]())
          push(span)
      }
    }
  }

  override def pop(): Option[Span] = {
    Option(threadLocalCtx.get).map {
      stack =>
        // remove the current span in scope for this thread
        val spanId = stack.pop()

        // rolls back the mdc span to the parent if present, if none then it will be cleared
        setSpanMDC(stack.headOption.map(_.info.id))

        spanId
    }
  }

  /**
   * Clears the entire call stack for the thread
   */
  override def clear() = {
    if (threadLocalCtx != null) {
      threadLocalCtx.remove()
    }
    setSpanMDC(None)
  }
}
