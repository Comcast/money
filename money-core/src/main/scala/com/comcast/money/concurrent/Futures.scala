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

package com.comcast.money.concurrent

import akka.actor.ActorRef
import com.comcast.money.core.Money.tracer
import com.comcast.money.core.{ Money, Result, SpanId }
import com.comcast.money.internal.SpanFSMProtocol.Start
import com.comcast.money.internal.SpanLocal
import com.comcast.money.internal.SpanSupervisorProtocol.SpanMessage
import com.comcast.money.logging.TraceLogging

import scala.concurrent.duration.Duration
import scala.concurrent.{ CanAwait, ExecutionContext, Future }
import scala.reflect.ClassTag
import scala.util.{ Failure, Success, Try }

object Futures {

  lazy val spanSupervisorRef: ActorRef = Money.tracer.spanSupervisorRef

  /**
   * Starts and stops a new trace span around a future and any chained methods attached to the future
   * @param name The name of the span
   * @param f The function body that will be run in a future
   * @param ec The execution context to run the future in
   * @tparam T The return type of the function body
   * @return A TracedFuture that wraps a Future instance created from the function body provided.  This allows
   *         us to propagate the same trace context to all functions attached to this future (e.g. map, flatMap)
   */
  def newTrace[T](name: String)(f: => T)(implicit ec: ExecutionContext): Future[T] = {

    // Creates a new trace context using the current span as the parent
    val spanId = newChildSpan()

    // Return a new future, providing the trace context which now holds the span id that the future should use
    new TracedFuture[T](
      spanId, Future(
      {
        spanSupervisorRef ! SpanMessage(spanId, Start(spanId, name))
        SpanLocal.push(spanId)
        f
      }
    ), ec
    )
  }

  /**
   * Executes the future and any chained methods attached to the future inside of an existing trace context if one is
   * present
   * @param f The function body that will be run in a future
   * @param ec The execution context to run the future in
   * @tparam T The return type of the function body
   * @return A TracedFuture that wraps a Future instance created from the function body provided.
   */
  def continueTrace[T](f: => T)(implicit ec: ExecutionContext): Future[T] = {

    SpanLocal.current match {
      case Some(spanId) =>
        new TracedFuture[T](
          spanId, Future(
          {
            SpanLocal.push(spanId)
            f
          }
        ), ec
        )
      case None =>
        Future(f)
    }
  }

  /**
   * Wraps a block that returns a future inside of a traced future, allowing chained methods to use tracing if an
   * existing
   * trace context is present.  If no trace span is present, then the future will run normally
   * @param f The function body that creates a future
   * @param ec The execution context that the future an tracing will be run in
   * @tparam T The future type
   * @return A TracedFuture that wraps a Future instance
   */
  def wrapTrace[T](f: => Future[T])(implicit ec: ExecutionContext): Future[T] = {
    SpanLocal.current match {
      case Some(spanId) =>
        new TracedFuture[T](spanId, f, ec)
      case None =>
        f
    }
  }

  private def newChildSpan(): SpanId = SpanLocal.current match {
    case None =>
      SpanId()
    case Some(existingSpanId) =>
      SpanId(existingSpanId.traceId, existingSpanId.spanId)
  }
}

object TracedFuture {

  // creates a function body wrapping the function provided, but propagates the span id before running the function
  def wrapMap[T, S](spanId: SpanId, f: T => S)(implicit executor: ExecutionContext): T => S = {
    t: T =>
      try {
        SpanLocal.push(spanId)
        f(t)
      } finally {
        SpanLocal.clear()
      }
  }

  // creates a function body wrapping the function provided, but propagates the span id before running the function
  def wrapFlatMap[T, S](spanId: SpanId, f: T => Future[S])(implicit executor: ExecutionContext): T => Future[S] = {
    t: T =>
      try {
        SpanLocal.push(spanId)
        f(t)
      } finally {
        SpanLocal.clear()
      }
  }

  // creates a function body wrapping the function provided, but propagates the span id before running the function
  def wrapComplete[T, U](spanId: SpanId, f: Try[T] => U): Try[T] => U = {
    t: Try[T] =>
      try {
        SpanLocal.push(spanId)
        f(t)
      } finally {
        SpanLocal.clear()
      }
  }

  // creates a function body wrapping the function provided, but propagates the span id before running the function
  def wrapFailure(spanId: SpanId, f: Throwable => Throwable): Throwable => Throwable = {
    t: Throwable =>
      try {
        SpanLocal.push(spanId)
        f(t)
      } finally {
        SpanLocal.clear()
      }
  }

  // creates a partial function body wrapping the function provided, but propagates the span id before running the
  // function
  def wrapPartialFunction[S, U](spanId: SpanId, pf: PartialFunction[S, U]): PartialFunction[S, U] = {
    case v =>
      try {
        SpanLocal.push(spanId)
        pf(v)
      } finally {
        SpanLocal.clear()
      }
  }
}

/**
 * Main class that wraps a Scala Future.  This class exists to override and wrap every method that exists on a Scala
 * Future.
 * This allows us to propagate the same trace context to be used not only by the main future, but any futures that
 * are built
 * using methods like map and flatMap off of the main future.
 *
 * This class maintains state in order to determine who is responsible ultimately for stopping a trace span
 * @param spanId The Span ID that this future, and all linked futures, should use for tracing
 * @param wrapped A Future that will be wrapped by this future
 * @tparam T The return type of the main future
 */
class TracedFuture[T](spanId: SpanId, wrapped: Future[T], executor: ExecutionContext)
    extends Future[T] with TraceLogging {
  self =>

  // This is critical, we have to tie an on complete every time we construct a new Traced Future
  // so that we will stop the span.  Remember, stopping the span doesn't stop it immediately, it hangs
  // out for some time.  Spans can now be stopped multiple times, the last stop result in wins
  onComplete {
    case Success(_) =>
      tracer.stopSpan(Result.success)
    case Failure(t) =>
      logException(t)
      tracer.stopSpan(Result.failed)
  }(executor)

  override def isCompleted: Boolean = wrapped.isCompleted

  override def value: Option[Try[T]] = wrapped.value

  override def result(atMost: Duration)(implicit permit: CanAwait): T = wrapped.result(atMost)

  override def ready(atMost: Duration)(implicit permit: CanAwait): this.type = {
    wrapped.ready(atMost)
    this
  }

  /**
   * All of the following functions wrap all of the functions that come from the Future trait.  What we do is
   * essentially
   * wrap any functions that come in on these methods with an appropriate wrapper function.  Each
   * wrapper function will do a SpanLocal.push with the correct span id, and then execute the intended
   * function body.
   */
  override def onComplete[U](@deprecatedName('func) f: Try[T] => U)(implicit executor: ExecutionContext): Unit = {
    wrapped.onComplete(TracedFuture.wrapComplete(spanId, f))
  }

  override def map[S](f: T => S)(implicit executor: ExecutionContext): Future[S] = {
    new TracedFuture(spanId, wrapped.map(TracedFuture.wrapMap(spanId, f)), executor)
  }

  override def flatMap[S](f: T => Future[S])(implicit executor: ExecutionContext): Future[S] = {
    new TracedFuture(spanId, wrapped.flatMap(TracedFuture.wrapFlatMap(spanId, f)), executor)
  }

  override def transform[S](s: T => S, f: Throwable => Throwable)(implicit executor: ExecutionContext): Future[S] = {
    new TracedFuture[S](
      spanId, wrapped.transform(TracedFuture.wrapMap(spanId, s), TracedFuture.wrapFailure(spanId, f)), executor
    )
  }

  override def foreach[U](f: T => U)(implicit executor: ExecutionContext): Unit = {
    onComplete {
      _ foreach f
    }
  }

  override def onFailure[U](callback: PartialFunction[Throwable, U])(implicit executor: ExecutionContext): Unit = onComplete {
    case Failure(t) =>
      callback.applyOrElse[Throwable, Any](t, Predef.conforms[Throwable]) // Exploiting the cached function to avoid
    // MatchError
    case _ =>
  }

  override def onSuccess[U](pf: PartialFunction[T, U])(implicit executor: ExecutionContext): Unit = onComplete {
    case Success(v) =>
      pf.applyOrElse[T, Any](v, Predef.conforms[T]) // Exploiting the cached function to avoid MatchError
    case _ =>
  }

  override def failed: Future[Throwable] = {
    new TracedFuture[Throwable](spanId.copy(), wrapped.failed, executor)
  }

  override def filter(pred: T => Boolean)(implicit executor: ExecutionContext): Future[T] =
    map {
      r => if (pred(r)) r else throw new NoSuchElementException("Future.filter predicate is not satisfied")
    }

  override def collect[S](pf: PartialFunction[T, S])(implicit executor: ExecutionContext): Future[S] =
    map {
      r =>
        pf.applyOrElse(
          r, (t: T) => throw new NoSuchElementException("Future.collect partial function is not defined at: " + t)
        )
    }

  override def recover[U >: T](pf: PartialFunction[Throwable, U])(implicit executor: ExecutionContext): Future[U] = {
    new TracedFuture[U](spanId, wrapped.recover(TracedFuture.wrapPartialFunction(spanId, pf)), executor)
  }

  override def recoverWith[U >: T](pf: PartialFunction[Throwable, Future[U]])(implicit executor: ExecutionContext): Future[U] = {
    new TracedFuture[U](spanId, wrapped.recoverWith(TracedFuture.wrapPartialFunction(spanId, pf)), executor)
  }

  override def andThen[U](pf: PartialFunction[Try[T], U])(implicit executor: ExecutionContext): Future[T] = {
    new TracedFuture[T](spanId, wrapped.andThen(TracedFuture.wrapPartialFunction(spanId, pf)), executor)
  }

  /**
   * Functions zip, fallbackTo, and mapTo are goofy, in that they take an already created future
   * instead of a function body.  Because of that, the future may already have been completed
   * by the time these functions are called.  May need to revisit these to make sure their behavior is correct.
   */
  override def zip[U](that: Future[U]): Future[(T, U)] = {

    new TracedFuture[(T, U)](spanId, wrapped.zip(that), executor)
  }

  override def fallbackTo[U >: T](that: Future[U]): Future[U] = {

    if (that.isInstanceOf[TracedFuture[U]]) {
      wrapped.fallbackTo(that)
    } else {
      new TracedFuture[U](spanId, wrapped.fallbackTo(that), executor)
    }
  }

  override def mapTo[S](implicit tag: ClassTag[S]): Future[S] = {

    new TracedFuture[S](spanId, wrapped.mapTo(tag), executor)
  }
}
