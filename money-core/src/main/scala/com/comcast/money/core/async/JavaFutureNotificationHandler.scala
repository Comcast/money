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

package com.comcast.money.core.async

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{ CompletableFuture, ExecutionException, Future, TimeUnit }

import scala.annotation.tailrec
import scala.util.{ Failure, Success, Try }
import ExecutionExceptionUnwrapper._

class JavaFutureNotificationHandler extends AbstractAsyncNotificationHandler[Future[_]] {
  val completableFutureNotificationHandler = new CompletableFutureNotificationHandler

  override def whenComplete(future: Future[_])(f: Try[_] => Unit): Future[_] =
    future match {
      case completableFuture: CompletableFuture[_] =>
        completableFutureNotificationHandler.whenComplete(completableFuture)(f)
      case _ if future.isDone =>
        try {
          f(Success(future.get()))
        } catch {
          case exception: Throwable =>
            f(Failure(unwrapExecutionException(exception)))
        }
        future
      case _ => TracingFuture(future, f)
    }
}

case class TracingFuture(future: Future[_], f: Try[_] => Unit) extends Future[Any] {
  private val completed = new AtomicBoolean(false)

  override def cancel(mayInterruptIfRunning: Boolean): Boolean = future.cancel(mayInterruptIfRunning)

  override def isCancelled: Boolean = {
    future.isCancelled match {
      case true if !completed.get() =>
        notifyDone()
        true
      case other: Boolean => other
    }
  }

  override def isDone: Boolean = {
    future.isDone match {
      case true if !completed.get() =>
        notifyDone()
        true
      case other: Boolean => other
    }
  }

  override def get(): Any = {
    try {
      notifyResult(future.get())
    } catch {
      case exception: ExecutionException =>
        notifyException(exception)
        throw exception
    }
  }

  override def get(timeout: Long, unit: TimeUnit): Any = {
    try {
      notifyResult(future.get(timeout, unit))
    } catch {
      case exception: ExecutionException =>
        notifyException(exception)
        throw exception
    }
  }

  override def hashCode(): Int = future.hashCode()
  override def equals(obj: Any): Boolean = future.equals(obj)
  override def toString: String = future.toString

  private def notifyDone(): Unit = {
    try {
      notifyResult(future.get())
    } catch {
      case exception: ExecutionException =>
        notifyException(exception)
    }
  }

  private def notifyResult(result: Any): Any = {
    if (completed.compareAndSet(false, true)) f(Success(result))
    result
  }

  private def notifyException(exception: Throwable): Unit =
    if (completed.compareAndSet(false, true)) f(Failure(unwrapExecutionException(exception)))
}

private object ExecutionExceptionUnwrapper {
  @tailrec
  def unwrapExecutionException(exception: Throwable): Throwable = {
    lazy val cause = exception.getCause
    if (!exception.isInstanceOf[ExecutionException] || cause == null) {
      exception
    } else {
      unwrapExecutionException(cause)
    }
  }
}