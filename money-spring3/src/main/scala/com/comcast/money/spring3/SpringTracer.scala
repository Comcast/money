package com.comcast.money.spring3

import akka.actor.ActorRef
import com.comcast.money.core._
import org.springframework.stereotype.Component

/**
 * Simply wraps the existing money tracer so it can be injected.  Delegates all calls to the configured
 * Money tracer in the event that Money or tracing is disabled
 */
@Component
class SpringTracer extends Tracer {

  override val spanSupervisorRef: ActorRef = Money.tracer.spanSupervisorRef
  private var tracer = Money.tracer

  private[spring3] def setTracer(toSet: Tracer): Unit =
    tracer = toSet

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
  override def startSpan(key: String): Unit = tracer.startSpan(key)

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
  override def time(key: String): Unit = tracer.time(key)

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
  override def record(key: String, measure: Double): Unit = tracer.record(key, measure)

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
  override def record(key: String, measure: Double, propogate: Boolean): Unit = tracer.record(key, measure, propogate)

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
  override def record(key: String, measure: String): Unit = tracer.record(key, measure)

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
  override def record(key: String, measure: String, propogate: Boolean): Unit = tracer.record(key, measure, propogate)

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
  override def record(key: String, measure: Long): Unit = tracer.record(key, measure)

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
  override def record(key: String, measure: Long, propogate: Boolean): Unit = tracer.record(key, measure, propogate)

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
  override def record(key: String, measure: Boolean): Unit = tracer.record(key, measure)

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
  override def record(key: String, measure: Boolean, propogate: Boolean): Unit = tracer.record(key, measure, propogate)

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
  override def record(note: Note[_]): Unit = tracer.record(note)

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
  override def record(note: Note[_], propogate: Boolean): Unit = tracer.record(note, propogate)

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
  override def stopSpan(result: Note[Boolean]): Unit = tracer.stopSpan(result)

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
  override def startTimer(key: String): Unit = tracer.startTimer(key)

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
  override def stopTimer(key: String): Unit = tracer.stopTimer(key)

  override def close(): Unit = tracer.close()
}
