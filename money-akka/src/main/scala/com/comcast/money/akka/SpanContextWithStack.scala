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

package com.comcast.money.akka

import com.comcast.money.api.Span
import com.comcast.money.core.internal.SpanContext

/**
 * A [[SpanContext]] that carries with it a stack of [[Span]] enables explicitly passing of the [[SpanContext]].
 *
 * A [[com.comcast.money.core.internal.SpanLocal]] would not be appropriate as it is tied to a single thread.
 *
 * BEWARE: The below is intended to be used through [[com.comcast.money.core.Tracer]] not to be called directly
 *
 * DO NOT DIRECTLY MUTATE THE STACK IF YOU WANT THE SPAN TO BE CORRECTLY TRACKED
 *
 */

class SpanContextWithStack() extends SpanContext {
  /**
   * stack is a mutable [[List]] of Spans.
   */

  private var stack: List[Span] = List.empty[Span]

  /**
   * Returns a fresh copy of this [[SpanContextWithStack]].
   *
   * @return SpanContextWithStack
   */

  def copy: SpanContextWithStack = {
    val freshSpanContext = new SpanContextWithStack
    freshSpanContext.setAll(replacementSpans = this.getAll)
  }

  /**
   * INTERNAL API
   *
   * replaces all spans in the [[SpanContextWithStack]] necessary only for the copy method to correctly copy.
   *
   * @param replacementSpans spans to replace existing spans
   * @return this SpanContextWithStack
   */

  private def setAll(replacementSpans: List[Span]): SpanContextWithStack = {
    stack = replacementSpans
    this
  }

  /**
   * Returns all spans currently stored `spans`.
   * @return List[Span]
   */

  def getAll: List[Span] = stack

  /**
   * CAUTION THIS METHOD WILL NOT TRACK THE SPAN
   *
   * Returns Unit
   *
   * Adds a [[Span]] to the top of the Stack
   *
   * @param span the [[Span]] to be prepended
   */

  override def push(span: Span): Unit = stack = span :: stack

  /**
   * CAUTION THIS METHOD WILL NOT TRACK THE SPAN
   *
   * Returns the last element inserted and removes it
   *
   * [[Some]] when Stack has at least one element
   *
   * [[None]] when Stack is empty
   *
   * @return Option[Span]
   */

  override def pop(): Option[Span] =
    stack.headOption match {
      case someSpan: Some[Span] =>
        stack = stack.tail
        someSpan

      case None => None
    }

  /**
   * Returns the last element inserted
   *
   * [[Some]] when Stack has at least one element
   *
   * [[None]] when Stack is empty
   *
   * @return
   */

  override def current: Option[Span] = stack.headOption

  /**
   * CAUTION THIS WILL CAUSE ALL THE STARTED SPANS TO NOT BE STOPPED
   *
   * Returns Unit
   *
   * empties the Stack of all spans
   */

  override def clear(): Unit = stack = List.empty[Span]
}
