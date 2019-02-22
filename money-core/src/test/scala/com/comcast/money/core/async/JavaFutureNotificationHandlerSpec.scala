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

import java.util.concurrent.{ CompletableFuture, Future, TimeUnit }

import com.comcast.money.core.SpecHelpers
import com.comcast.money.core.concurrent.ConcurrentSupport
import org.mockito.Matchers.{ any, eq => argEq }
import org.mockito.Mockito.{ doReturn, never, times, verify }
import org.scalatest.{ Matchers, OneInstancePerTest, WordSpecLike }
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.ExecutionException
import scala.util.{ Failure, Try }

class JavaFutureNotificationHandlerSpec
  extends WordSpecLike
  with MockitoSugar with Matchers with ConcurrentSupport with OneInstancePerTest with SpecHelpers {

  val underTest = new JavaFutureNotificationHandler()

  "JavaFutureNotificationHandler" should {
    "support java.util.concurrent.Future" in {
      val future = mock[Future[_]]
      val result = underTest.supports(classOf[Future[_]], future)

      result shouldEqual true
    }
    "support descendents of java.util.concurrent.Future" in {
      val future = mock[MyFuture[String]]
      val result = underTest.supports(classOf[Future[_]], future)

      result shouldEqual true
    }
    "does not support descendent classes of java.util.concurrent.Future" in {
      val future = mock[CompletableFuture[_]]
      val result = underTest.supports(classOf[CompletableFuture[_]], future)

      result shouldEqual false
    }
    "does not support non-Futures" in {
      val nonFuture = new Object()
      val result = underTest.supports(classOf[Object], nonFuture)

      result shouldEqual false
    }
    "does not support null class" in {
      val future = mock[Future[_]]
      val result = underTest.supports(null, future)

      result shouldEqual false
    }
    "does not support null" in {
      val result = underTest.supports(classOf[Future[_]], null)

      result shouldEqual false
    }
    "calls registered completion function for already completed future" in {
      val future = new WrappedFuture[String](CompletableFuture.completedFuture("success"))
      val func = mock[Try[_] => Unit]

      underTest.whenComplete(classOf[Future[String]], future)(func)

      verify(func, times(1)).apply(argEq(Try("success")))
    }
    "calls registered completion function for already exceptionally completed future" in {
      val ex = new RuntimeException
      val completableFuture = new CompletableFuture[String]()
      completableFuture.completeExceptionally(ex)
      val future = new WrappedFuture[String](completableFuture)
      val func = mock[Try[_] => Unit]

      underTest.whenComplete(classOf[Future[_]], future)(func)

      verify(func, times(1)).apply(argEq(Failure(ex)))
    }
    "calls registered completion function when the future completes successfully" in {
      val completableFuture = new CompletableFuture[String]()
      val future = new WrappedFuture[String](completableFuture)

      val func = mock[Try[_] => Unit]

      val wrapped = underTest.whenComplete(classOf[Future[String]], future)(func).asInstanceOf[Future[String]]
      verify(func, never()).apply(any())

      completableFuture.complete("success")
      verify(func, never()).apply(any())

      val _ = wrapped.get()

      verify(func, times(1)).apply(argEq(Try("success")))
    }
    "calls registered completion function when the future completes exceptionally" in {
      val completableFuture = new CompletableFuture[String]()
      val future = new WrappedFuture[String](completableFuture)

      val func = mock[Try[_] => Unit]

      val wrapped = underTest.whenComplete(classOf[Future[String]], future)(func).asInstanceOf[Future[String]]
      verify(func, never()).apply(any())

      val exception = new RuntimeException()
      completableFuture.completeExceptionally(exception)
      verify(func, never()).apply(any())

      assertThrows[ExecutionException] {
        val _ = wrapped.get()
      }

      verify(func, times(1)).apply(argEq(Failure(exception)))
    }
  }

  private abstract class MyFuture[T] extends Future[T] {}

  private final class WrappedFuture[T](parent: CompletableFuture[T]) extends Future[T] {
    override def cancel(mayInterruptIfRunning: Boolean): Boolean = parent.cancel(mayInterruptIfRunning)
    override def isCancelled: Boolean = parent.isCancelled
    override def isDone: Boolean = parent.isDone
    override def get(): T = parent.get()
    override def get(timeout: Long, unit: TimeUnit): T = parent.get(timeout, unit)
  }
}
