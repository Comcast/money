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

import java.io.Closeable
import java.util.UUID

import akka.actor._
import com.comcast.money.internal.SpanFSMProtocol._
import com.comcast.money.internal.SpanLocal
import com.comcast.money.internal.SpanSupervisorProtocol._
import com.comcast.money.util.DateTimeUtil

import scala.util.{ Random, Try }

object ProbabilisticlyUniqueLong {
  val r: Random = new Random()

  def apply() = r.nextLong()
}

object GUID {
  def apply() = UUID.randomUUID().toString
}

case class SpanId(traceId: String, parentId: Long, selfId: Long) {

  private val HttpHeaderFormat = "trace-id=%s;parent-id=%s;span-id=%s"
  private val StringFormat = "SpanId~%s~%s~%s"

  override def toString = StringFormat.format(traceId, parentId, selfId)

  def toHttpHeader = HttpHeaderFormat.format(traceId, parentId, selfId)
}

object SpanId {

  /**
   * Given a string representation of a span id, parses and returns the span id
   * @param spanId The string representation of a SpanId
   * @return a parsed SpanId in a Try.  Will return a Failure if the span id could not be parsed
   */
  def apply(spanId: String): Try[SpanId] = Try {
    // TODO: should have a better SerDe for this
    val parts = spanId.split('~')

    // last three form the span id
    SpanId(parts(1), parts(2).toLong, parts(3).toLong)
  }

  def apply(): SpanId = {
    val spanId = ProbabilisticlyUniqueLong()
    SpanId(GUID(), spanId, spanId)
  }

  //for testing
  def apply(spanId: Long): SpanId = SpanId(spanId.toString, spanId, spanId)

  def apply(originSpanId: String, parentSpanId: Long): SpanId = {
    val spanId = ProbabilisticlyUniqueLong()
    SpanId(originSpanId, parentSpanId, spanId)
  }

  def fromHttpHeader(httpHeader: String) = Try {
    val parts = httpHeader.split(';')
    val traceId = parts(0).split('=')(1)
    val parentId = parts(1).split('=')(1)
    val selfId = parts(2).split('=')(1)

    SpanId(traceId, parentId.toLong, selfId.toLong)
  }
}

/**
 * Primary API to be used for tracing
 */
trait Tracer extends Closeable {

  val spanSupervisorRef: ActorRef

  /**
   * Creates a new span if one is not present; or creates a child span for the existing Span if one is present
   *
   * {{{
   *   import com.comcast.money.core.Money._
   *   def somethingMeaningful() {
   *     try {
   *      tracer.startSpan("something")
   *      ...
   *    } finally {
   *      tracer.stopSpan()
   *    }
   *  }
   * }}}
   * @param key an identifier for the span
   */
  def startSpan(key: String) = {
    SpanLocal.current match {
      case Some(parentSpanId) =>
        val subSpanId = SpanId(parentSpanId.traceId, parentSpanId.selfId)
        start(key, subSpanId, Some(parentSpanId))
      case None =>
        val newSpanId = SpanId()
        start(key, newSpanId)
    }
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
  def time(key: String) = withSpanId { spanId =>
    spanSupervisorRef ! SpanMessage(spanId, AddNote(LongNote(key, Some(DateTimeUtil.microTime))))
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
  def record(key: String, measure: Double): Unit = withSpanId { spanId =>
    spanSupervisorRef ! SpanMessage(spanId, AddNote(DoubleNote(key, Option(measure))))
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
   * @param propogate propogate to children
   */
  def record(key: String, measure: Double, propogate: Boolean): Unit = withSpanId { spanId =>
    spanSupervisorRef ! SpanMessage(spanId, AddNote(DoubleNote(key, Option(measure)), propogate))
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
  def record(key: String, measure: String): Unit = withSpanId { spanId =>
    spanSupervisorRef ! SpanMessage(spanId, AddNote(StringNote(key, Option(measure))))
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
   * @param propogate propogate to children
   */
  def record(key: String, measure: String, propogate: Boolean): Unit = withSpanId { spanId =>
    spanSupervisorRef ! SpanMessage(spanId, AddNote(StringNote(key, Option(measure)), propogate))
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
  def record(key: String, measure: Long): Unit = withSpanId { spanId =>
    spanSupervisorRef ! SpanMessage(spanId, AddNote(LongNote(key, Option(measure))))
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
   * @param propogate propogate to children
   */
  def record(key: String, measure: Long, propogate: Boolean): Unit = withSpanId { spanId =>
    spanSupervisorRef ! SpanMessage(spanId, AddNote(LongNote(key, Option(measure)), propogate))
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
  def record(key: String, measure: Boolean): Unit = withSpanId { spanId =>
    spanSupervisorRef ! SpanMessage(spanId, AddNote(BooleanNote(key, Option(measure))))
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
   * @param propogate propogate to children
   */
  def record(key: String, measure: Boolean, propogate: Boolean): Unit = withSpanId { spanId =>
    spanSupervisorRef ! SpanMessage(spanId, AddNote(BooleanNote(key, Option(measure)), propogate))
  }

  /**
   * Adds a new [[com.comcast.money.core.Note]] directly to the current Span if one is present in context.
   * {{{
   *   import com.comcast.money.core.Money._
   *   def recordMe() {
   *     ...
   *     tracer.record(Result.success)
   *     tracer.record(Note("that", "thang"))
   *     ...
   *  }
   * }}}
   * @param note the [[com.comcast.money.core.Note]] to be added
   */
  def record(note: Note[_]) = withSpanId { spanId =>
    spanSupervisorRef ! SpanMessage(spanId, AddNote(note))
  }

  /**
   * Adds a new [[com.comcast.money.core.Note]] directly to the current Span if one is present in context.
   * {{{
   *   import com.comcast.money.core.Money._
   *   def recordMe() {
   *     ...
   *     tracer.record(Result.success)
   *     tracer.record(Note("that", "thang"))
   *     ...
   *  }
   * }}}
   * @param note the [[com.comcast.money.core.Note]] to be added
   */
  def record(note: Note[_], propogate: Boolean) = withSpanId { spanId =>
    spanSupervisorRef ! SpanMessage(spanId, AddNote(note, propogate))
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
   */
  def stopSpan(result: Note[Boolean] = Result.success) = withSpanId { spanId =>
    SpanLocal.pop()
    spanSupervisorRef ! SpanMessage(spanId, Stop(result, DateTimeUtil.microTime))
  }

  /**
   * Starts a new timer on the current Span for the key provided
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
   */
  def startTimer(key: String) = withSpanId { spanId =>
    spanSupervisorRef ! SpanMessage(spanId, StartTimer(key))
  }

  /**
   * Stops the timer on the current Span for the key provided.  This method assumes that a timer was started for the
   * key, ususally used
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
   */
  def stopTimer(key: String) = withSpanId { spanId =>
    spanSupervisorRef ! SpanMessage(spanId, StopTimer(key))
  }

  override def close() = stopSpan()

  private def start(key: String, spanId: SpanId, parentSpanIdOpt: Option[SpanId] = None): Unit = {
    SpanLocal.push(spanId)
    spanSupervisorRef ! SpanMessage(spanId, Start(spanId, key, parentSpanId = parentSpanIdOpt))
  }

  private def withSpanId(func: SpanId => Unit): Unit = {
    SpanLocal.current.map(func)
  }
}
