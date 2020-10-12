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

package com.comcast.money.core

import java.io.Closeable

import com.comcast.money.api.{ MoneyTracer, Note, Span, SpanFactory }
import com.comcast.money.core.internal.{ SpanContext, SpanLocal }
import io.opentelemetry.context.Scope
import io.opentelemetry.trace.{ StatusCanonicalCode, Span => OtelSpan }

/**
 * Primary API to be used for tracing
 */
trait Tracer extends MoneyTracer with Closeable {

  val spanFactory: SpanFactory

  val spanContext: SpanContext = SpanLocal

  override def getCurrentSpan: Span = spanContext.current.getOrElse(DisabledSpan)

  override def withSpan(span: OtelSpan): Scope = span match {
    case moneySpan: Span => withSpan(moneySpan)
    case _ => throw new IllegalArgumentException("span is not a compatible Money span")
  }

  override def withSpan(span: Span): Scope = spanContext.push(span)

  override def spanBuilder(spanName: String): Span.Builder = new CoreSpanBuilder(spanContext.current, spanName, spanFactory)

  /**
   * Creates a new span if one is not present; or creates a child span for the existing Span if one is present
   *
   * {{{
   *   import com.comcast.money.core.Money._
   *   def somethingMeaningful() {
   *     val scope = tracer.startSpan("something")
   *     try {
   *       ...
   *     } finally {
   *       scope.close()
   *     }
   *   }
   * }}}
   *
   * @param key an identifier for the span
   */
  def startSpan(key: String): Span = {

    val child = spanContext.current
      .map { existingSpan =>
        spanFactory.childSpan(key, existingSpan)
      }
      .getOrElse(
        spanFactory.newSpan(key))

    val scope = spanContext.push(child)
    child.start()
    child.attachScope(scope)
  }

  /**
   * Captures a timestamp for the key provided on the current Span if present.  If a Span is present, a Note
   * will be added to the Span.
   * {{{
   *   import com.comcast.money.core.Money._
   *   def timeMe() {
   *     ...
   *     tracer.time("something")
   *     ...
   *  }
   * }}}
   * @param key the identifier for the timestamp being captured
   */
  def time(key: String): Unit = withCurrentSpan { span =>
    span.record(Note.of(key, System.currentTimeMillis()))
  }

  /**
   * Captures an arbitrary data element on the current Span if present.  If a span is present, a Note
   * will be added to the Span with the key and data element provided
   * {{{
   *   import com.comcast.money.core.Money._
   *   def recordMe() {
   *     ...
   *     tracer.record("that", "thang")
   *     ...
   *  }
   * }}}
   * @param key the identifier for the data being captured
   * @param measure the value being captured
   */
  def record(key: String, measure: Double): Unit = withCurrentSpan { span =>
    span.record(Note.of(key, measure))
  }

  /**
   * Captures an arbitrary data element on the current Span if present.  If a span is present, a Note
   * will be added to the Span with the key and data element provided
   * {{{
   *   import com.comcast.money.core.Money._
   *   def recordMe() {
   *     ...
   *     tracer.record("that", "thang")
   *     ...
   *  }
   * }}}
   * @param key the identifier for the data being captured
   * @param measure the value being captured
   * @param propagate propagate to children
   */
  def record(key: String, measure: Double, propagate: Boolean): Unit = withCurrentSpan { span =>
    span.record(Note.of(key, measure, propagate))
  }

  /**
   * Captures an arbitrary data element on the current Span if present.  If a span is present, a Note
   * will be added to the Span with the key and data element provided
   * {{{
   *   import com.comcast.money.core.Money._
   *   def recordMe() {
   *     ...
   *     tracer.record("that", "thang")
   *     ...
   *  }
   * }}}
   * @param key the identifier for the data being captured
   * @param measure the value being captured
   */
  def record(key: String, measure: String): Unit = withCurrentSpan { span =>
    span.record(Note.of(key, measure))
  }

  /**
   * Captures an arbitrary data element on the current Span if present.  If a span is present, a Note
   * will be added to the Span with the key and data element provided
   * {{{
   *   import com.comcast.money.core.Money._
   *   def recordMe() {
   *     ...
   *     tracer.record("that", "thang")
   *     ...
   *  }
   * }}}
   * @param key the identifier for the data being captured
   * @param measure the value being captured
   * @param propagate propagate to children
   */
  def record(key: String, measure: String, propagate: Boolean): Unit = withCurrentSpan { span =>
    span.record(Note.of(key, measure, propagate))
  }

  /**
   * Captures an arbitrary data element on the current Span if present.  If a span is present, a Note
   * will be added to the Span with the key and data element provided
   * {{{
   *   import com.comcast.money.core.Money._
   *   def recordMe() {
   *     ...
   *     tracer.record("that", "thang")
   *     ...
   *  }
   * }}}
   * @param key the identifier for the data being captured
   * @param measure the value being captured
   */
  def record(key: String, measure: Long): Unit = withCurrentSpan { span =>
    span.record(Note.of(key, measure))
  }

  /**
   * Captures an arbitrary data element on the current Span if present.  If a span is present, a Note
   * will be added to the Span with the key and data element provided
   * {{{
   *   import com.comcast.money.core.Money._
   *   def recordMe() {
   *     ...
   *     tracer.record("that", "thang")
   *     ...
   *  }
   * }}}
   * @param key the identifier for the data being captured
   * @param measure the value being captured
   * @param propagate propagate to children
   */
  def record(key: String, measure: Long, propagate: Boolean): Unit = withCurrentSpan { span =>
    span.record(Note.of(key, measure, propagate))
  }

  /**
   * Captures an arbitrary data element on the current Span if present.  If a span is present, a Note
   * will be added to the Span with the key and data element provided
   * {{{
   *   import com.comcast.money.core.Money._
   *   def recordMe() {
   *     ...
   *     tracer.record("that", "thang")
   *     ...
   *  }
   * }}}
   * @param key the identifier for the data being captured
   * @param measure the value being captured
   */
  def record(key: String, measure: Boolean): Unit = withCurrentSpan { span =>
    span.record(Note.of(key, measure))
  }

  /**
   * Captures an arbitrary data element on the current Span if present.  If a span is present, a Note
   * will be added to the Span with the key and data element provided
   * {{{
   *   import com.comcast.money.core.Money._
   *   def recordMe() {
   *     ...
   *     tracer.record("that", "thang")
   *     ...
   *  }
   * }}}
   * @param key the identifier for the data being captured
   * @param measure the value being captured
   * @param propagate propagate to children
   */
  def record(key: String, measure: Boolean, propagate: Boolean): Unit = withCurrentSpan { span =>
    span.record(Note.of(key, measure, propagate))
  }

  /**
   * Adds a new [[com.comcast.money.api.Note]] directly to the current Span if one is present in context.
   * {{{
   *   import com.comcast.money.core.Money._
   *   def recordMe() {
   *     ...
   *     tracer.record(Result.success)
   *     tracer.record(Note("that", "thang"))
   *     ...
   *  }
   * }}}
   * @param note the [[com.comcast.money.api.Note]] to be added
   */
  def record(note: Note[_]): Unit = withCurrentSpan { span =>
    span.record(note)
  }

  /**
   * Stops the current span, adding a note that indicates whether it succeeded or failed.
   * {{{
   *   import com.comcast.money.core.Money._
   *   def somethingMeaningful() {
   *     try {
   *      tracer.startSpan("something")
   *      ...
   *    } finally {
   *      tracer.stopSpan(Result.success)
   *    }
   *  }
   * }}}
   * @param result The result of the span, this will be Result.success or Result.failed
   * @deprecated Close the [[Scope]] returned from [[Tracer.startSpan()]]
   */
  @deprecated
  @Deprecated
  def stopSpan(result: Boolean = true): Unit = {
    spanContext.current.foreach { span =>
      span.setStatus(if (result) StatusCanonicalCode.OK else StatusCanonicalCode.ERROR)
      span.close()
    }
  }

  /**
   * Starts a new timer on the current Span for the key provided
   * {{{
   * import com.comcast.money.core.Money._
   *
   * def timeThisChumpie() {
   *   val scope = tracer.startTimer("chumpie")
   *   try {
   *     ...
   *   } finally {
   *     scope.close()
   *   }
   * }
   * }}}
   * @param key the identifier for the timer
   */
  def startTimer(key: String): Scope = spanContext.current
    .map { _.startTimer(key) }
    .getOrElse(() => ())

  /**
   * Stops the timer on the current Span for the key provided.  This method assumes that a timer was started for the
   * key, usually used
   * in conjunction with `startTimer`
   * {{{
   * import com.comcast.money.core.Money._
   *
   * def timeThisChumpie() {
   *   try {
   *     tracer.startTimer("chumpie")
   *     ...
   *   } finally {
   *     tracer.stopTimer("chumpie")
   *   }
   * }
   * }}}
   * @param key the identifier for the timer
   * @deprecated Close the [[Scope]] returned from [[Tracer.startTimer()]]
   */
  @deprecated
  @Deprecated
  def stopTimer(key: String): Unit = withCurrentSpan { span =>
    span.stopTimer(key)
  }

  override def close(): Unit = stopSpan()

  private def withCurrentSpan(func: Span => Unit): Unit =
    spanContext.current.foreach(func)
}
