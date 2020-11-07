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
import io.opentelemetry.context.{ Context, Scope }
import org.slf4j.{ Logger, LoggerFactory }

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

private[akka] object SpanContextWithStack {
  private val log: Logger = LoggerFactory.getLogger(classOf[SpanContextWithStack])

  def logSpanMismatch(expected: Span, actual: Span): Unit = {
    log.error("Unexpected span scope being closed.  Expected={} Actual={}", expected, actual, new Throwable().fillInStackTrace())
  }
}

class SpanContextWithStack() extends SpanContext {
  /**
   * stack is a mutable [[List]] of Spans.
   */

  private var stack: List[Span] = Nil

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

  override def push(span: Span): Scope = {
    import com.comcast.money.akka.SpanContextWithStack.logSpanMismatch

    // capture the previous stack of spans
    val previousStack = stack
    // push the new span to the head of the stack
    stack = span :: previousStack

    () => stack match {
      // happy path, the remaining stack of spans matches the captured stack of spans
      case _ :: rest if rest == previousStack =>
        stack = rest
      // unbalanced state, log the expected/actual head span and restore the stack to the captured stack of spans
      case head :: _ =>
        logSpanMismatch(span, head)
        stack = previousStack
      // unbalanced state, log the expected head span and leave the stack of spans empty
      case Nil =>
        logSpanMismatch(span, null)
    }
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

  override def fromContext(context: Context): Option[Span] = current
}
