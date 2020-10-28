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
import io.opentelemetry.context.{ Context, Scope }
import io.opentelemetry.api.trace.{ Span => OtelSpan }

trait SpanContext {
  def push(span: Span): Scope
  def current: Option[Span]
  def fromContext(context: Context): Option[Span]
}

/**
 * Provides a thread local context for storing SpanIds.  Keeps a stack of trace ids so that we
 * an roll back to the parent once a span completes
 */
object SpanLocal extends SpanContext {

  private lazy val mdcSupport = new MDCSupport()

  import mdcSupport._

  override def current: Option[Span] = fromContext(Context.current)

  override def push(span: Span): Scope =
    if (span != null) {
      val context = Context.current
      val updatedContext = span.storeInContext(context)
      val scope = updatedContext.makeCurrent()
      setSpanMDC(Some(span))
      () => {
        scope.close()
        setSpanMDC(current)
      }
    } else () => ()

  def fromContext(context: Context): Option[Span] =
    Option(OtelSpan.fromContextOrNull(context)) match {
      case Some(span: Span) => Some(span)
      case _ => None
    }

  def clear(): Unit = {
    Context.root.makeCurrent()
    setSpanMDC(None)
  }
}
