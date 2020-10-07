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

import com.comcast.money.api.Span
import io.grpc.Context
import io.opentelemetry.trace.TracingContextUtils

import scala.annotation.tailrec

trait SpanContext {
  def push(span: Span): Unit
  def pop(): Option[Span]
  def current: Option[Span]
  def clear(): Unit
}

/**
 * Provides a thread local context for storing SpanIds.  Keeps a stack of trace ids so that we
 * an roll back to the parent once a span completes
 */
object SpanLocal extends SpanContext {

  // A stack of previous Context instances
  private[this] val threadLocalContexts = ThreadLocal.withInitial[List[Context]](() => Nil)

  private lazy val mdcSupport = new MDCSupport()

  import mdcSupport._

  override def current: Option[Span] = fromContext(Context.current)

  override def push(span: Span): Unit =
    if (span != null) {
      val updatedContext = TracingContextUtils.withSpan(span, Context.current)
      val previousContext = updatedContext.attach()
      threadLocalContexts.set(previousContext :: threadLocalContexts.get)
      setSpanMDC(Some(span))
    }

  override def pop(): Option[Span] =
    (current, threadLocalContexts.get) match {
      case (Some(span), previousContext :: rest) =>
        threadLocalContexts.set(rest)
        Context.current.detach(previousContext)
        setSpanMDC(fromContext(previousContext))
        Some(span)
      case _ => None
    }

  /**
   * Clears the entire call stack for the thread
   */
  override def clear(): Unit = {
    threadLocalContexts.get.foreach { previousContext =>
      Context.current.detach(previousContext)
    }
    threadLocalContexts.set(Nil)
    setSpanMDC(None)
  }

  def fromContext(context: Context): Option[Span] =
    Option(TracingContextUtils.getSpanWithoutDefault(context)) match {
      case Some(span: Span) => Some(span)
      case _ => None
    }
}
