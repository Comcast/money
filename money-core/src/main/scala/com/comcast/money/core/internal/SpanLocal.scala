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

  // A stack of span ids for the current thread
  private[this] val threadLocalCtx = ThreadLocal.withInitial[List[Span]](() => Nil)

  private lazy val mdcSupport = new MDCSupport()

  import mdcSupport._

  override def current: Option[Span] = Option(threadLocalCtx.get).flatMap(_.headOption)

  override def push(span: Span): Unit =
    if (span != null) {
      threadLocalCtx.set(span :: threadLocalCtx.get())
      setSpanMDC(Some(span.info.id))
      setSpanNameMDC(Some(span.info.name))
    }

  override def pop(): Option[Span] =
    threadLocalCtx.get match {
      case Nil => None
      case span :: rest =>
        threadLocalCtx.set(rest)
        val head = rest.headOption
        setSpanMDC(head.map(_.info.id))
        setSpanNameMDC(head.map(_.info.name))
        Some(span)
    }

  /**
   * Clears the entire call stack for the thread
   */
  override def clear(): Unit = {
    threadLocalCtx.remove()

    setSpanMDC(None)
    setSpanNameMDC(None)
  }
}
