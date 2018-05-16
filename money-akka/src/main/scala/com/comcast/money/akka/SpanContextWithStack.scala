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

package com.comcast.money.akka

import com.comcast.money.api.Span
import com.comcast.money.core.internal.SpanContext

/**
 * A [[SpanContext]] that carries with it a stack of [[Span]]
 * enables explicitly passing of the [[SpanContext]].
 * A [[com.comcast.money.core.internal.SpanLocal]] would not be appropriate
 * as it tied to a single thread.
 */

class SpanContextWithStack() extends SpanContext {
  private var spans = Seq.empty[Span]

  private def setAll(oldSpans: Seq[Span]) = {
    spans = oldSpans
    this
  }

  def copy = {
    val freshSpanContext = new SpanContextWithStack
    freshSpanContext.setAll(this.getAll)
  }

  def getAll: Seq[Span] = spans

  override def push(span: Span): Unit = spans = span +: spans

  override def pop: Option[Span] =
    spans.headOption match {
      case someSpan: Some[Span] =>
        spans = spans.tail
        someSpan

      case None => None
    }

  override def current: Option[Span] = spans.headOption

  override def clear: Unit = spans = Seq.empty[Span]
}
