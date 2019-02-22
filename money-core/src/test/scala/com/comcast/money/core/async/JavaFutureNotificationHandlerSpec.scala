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

import java.util.concurrent.{CompletableFuture, Future, TimeUnit, TimeoutException}

import com.comcast.money.core.SpecHelpers
import com.comcast.money.core.concurrent.ConcurrentSupport
import org.mockito.Matchers.{any, eq => argEq}
import org.mockito.Mockito._
import org.scalatest.{Matchers, OneInstancePerTest, WordSpecLike}
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.ExecutionException
import scala.util.{Failure, Success, Try}

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
    "calls whenComplete function for already completed CompletableFuture" in {
      val future = spy(CompletableFuture.completedFuture("success"))
      val func = mock[Try[_] => Unit]

      underTest.whenComplete(classOf[Future[String]], future)(func)

      verify(future).whenComplete(any())
      verify(func, times(1)).apply(argEq(Try("success")))
    }
  }
  "TracingFuture" should {
    "delegates hashCode" in {
      val future = mock[Future[String]]
      val func = mock[Try[_] => Unit]

      val expected = future.hashCode()
      val wrapped = TracingFuture(future, func)

      val result = wrapped.hashCode()
      result shouldBe expected
    }
    "delegates toString" in {
      val future = mock[Future[String]]
      val func = mock[Try[_] => Unit]

      val expected = future.toString
      val wrapped = TracingFuture(future, func)

      val result = wrapped.toString()
      result shouldBe expected
    }
    "delegates equals" in {
      val future = mock[Future[String]]
      val func = mock[Try[_] => Unit]

      val wrapped = TracingFuture(future, func)

      val result = wrapped.equals(future)
      result shouldBe true
    }
    "delegates cancel" in {
      val future = mock[Future[String]]
      val func = mock[Try[_] => Unit]
      doReturn(true).when(future).cancel(true)

      val wrapped = TracingFuture(future, func)

      val result = wrapped.cancel(true)
      result shouldBe true
      verify(future).cancel(true)
    }
    "get triggers completion" in {
      val future = CompletableFuture.completedFuture("success")
      val func = mock[Try[_] => Unit]
      doReturn(()).when(func)(any[Try[_]])

      val wrapped = TracingFuture(future, func)

      val result = wrapped.get()
      result shouldBe "success"
      verify(func)(Success("success"))
    }
    "get triggers exceptional completion" in {
      val future = new CompletableFuture[String]()
      val exception = new RuntimeException
      future.completeExceptionally(exception)
      val func = mock[Try[_] => Unit]

      val wrapped = TracingFuture(future, func)

      assertThrows[ExecutionException] {
        wrapped.get()
      }
      verify(func)(Failure(exception))
    }
    "get interrupted does not trigger completion" in {
      val future = mock[Future[String]]
      doThrow(classOf[InterruptedException]).when(future).get
      val func = mock[Try[_] => Unit]

      val wrapped = TracingFuture(future, func)

      assertThrows[InterruptedException] {
        wrapped.get()
      }
      verifyZeroInteractions(func)
    }
    "get with timeout triggers completion" in {
      val future = CompletableFuture.completedFuture("success")
      val func = mock[Try[_] => Unit]

      val wrapped = TracingFuture(future, func)

      val result = wrapped.get(1, TimeUnit.SECONDS)
      result shouldBe "success"
      verify(func)(Success("success"))
    }
    "get with timeout triggers exceptional completion" in {
      val future = new CompletableFuture[String]()
      val exception = new RuntimeException
      future.completeExceptionally(exception)
      val func = mock[Try[_] => Unit]

      val wrapped = TracingFuture(future, func)

      assertThrows[ExecutionException] {
        val result = wrapped.get(1, TimeUnit.SECONDS)
      }
      verify(func)(Failure(exception))
    }
    "get with timeout interrupted does not trigger completion" in {
      val future = mock[Future[String]]
      doThrow(classOf[InterruptedException]).when(future).get
      val func = mock[Try[_] => Unit]

      val wrapped = TracingFuture(future, func)

      assertThrows[InterruptedException] {
        wrapped.get()
      }
      verifyZeroInteractions(func)
    }
    "get with timeout times out does not trigger completion" in {
      val future = mock[Future[String]]
      doThrow(classOf[TimeoutException]).when(future).get
      val func = mock[Try[_] => Unit]

      val wrapped = TracingFuture(future, func)

      assertThrows[TimeoutException] {
        wrapped.get()
      }
      verifyZeroInteractions(func)
    }
    "isDone triggers completion" in {
      val future = mock[Future[String]]
      val func = mock[Try[_] => Unit]
      doReturn(false).when(future).isDone

      val wrapped = TracingFuture(future, func)

      val result1 = wrapped.isDone
      result1 shouldBe false
      verifyZeroInteractions(func)

      doReturn(true).when(future).isDone
      doReturn("success").when(future).get

      val result2 = wrapped.isDone
      result2 shouldBe true
      verify(func)(Success("success"))
    }
    "isDone triggers exceptional completion" in {
      val future = mock[Future[String]]
      val func = mock[Try[_] => Unit]
      doReturn(false).when(future).isDone

      val wrapped = TracingFuture(future, func)

      val result1 = wrapped.isDone
      result1 shouldBe false
      verifyZeroInteractions(func)

      doReturn(true).when(future).isDone
      val exception = new RuntimeException
      doThrow(new ExecutionException(exception)).when(future).get

      val result2 = wrapped.isDone
      result2 shouldBe true
      verify(func)(Failure(exception))
    }
    "isCancelled triggers completion" in {
      val future = mock[Future[String]]
      val func = mock[Try[_] => Unit]
      doReturn(false).when(future).isCancelled

      val wrapped = TracingFuture(future, func)

      val result1 = wrapped.isCancelled
      result1 shouldBe false
      verifyZeroInteractions(func)

      doReturn(true).when(future).isCancelled
      doReturn("success").when(future).get

      val result2 = wrapped.isCancelled
      result2 shouldBe true
      verify(func)(Success("success"))
    }
    "isCancelled triggers exceptional completion" in {
      val future = mock[Future[String]]
      val func = mock[Try[_] => Unit]
      doReturn(false).when(future).isCancelled

      val wrapped = TracingFuture(future, func)

      val result1 = wrapped.isCancelled
      result1 shouldBe false
      verifyZeroInteractions(func)

      doReturn(true).when(future).isCancelled
      val exception = new RuntimeException
      doThrow(new ExecutionException(exception)).when(future).get

      val result2 = wrapped.isCancelled
      result2 shouldBe true
      verify(func)(Failure(exception))
    }
  }

  private abstract class MyFuture[T] extends Future[T] { }

  private final class WrappedFuture[T](parent: CompletableFuture[T]) extends Future[T] {
    override def cancel(mayInterruptIfRunning: Boolean): Boolean = parent.cancel(mayInterruptIfRunning)
    override def isCancelled: Boolean = parent.isCancelled
    override def isDone: Boolean = parent.isDone
    override def get(): T = parent.get()
    override def get(timeout: Long, unit: TimeUnit): T = parent.get(timeout, unit)
  }
}
