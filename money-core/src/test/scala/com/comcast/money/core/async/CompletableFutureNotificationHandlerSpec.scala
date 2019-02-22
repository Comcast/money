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

import java.util.concurrent.{ CompletableFuture, CompletionStage }

import com.comcast.money.core.SpecHelpers
import com.comcast.money.core.concurrent.ConcurrentSupport
import org.mockito.Matchers.{ any, eq => argEq }
import org.mockito.Mockito.{ doReturn, never, times, verify }
import org.scalatest.{ Matchers, OneInstancePerTest, WordSpecLike }
import org.scalatest.mockito.MockitoSugar

import scala.util.{ Failure, Try }

class CompletableFutureNotificationHandlerSpec
  extends WordSpecLike
  with MockitoSugar with Matchers with ConcurrentSupport with OneInstancePerTest with SpecHelpers {

  val underTest = new CompletableFutureNotificationHandler()
  val futureClass: Class[_] = classOf[CompletableFuture[_]]
  val success: CompletableFuture[String] = CompletableFuture.completedFuture("success")

  "CompletionStageNotificationHandler" should {
    "support java.util.concurrent.CompletableFuture" in {
      val result = underTest.supports(futureClass, success)

      result shouldEqual true
    }
    "support descendents of java.util.concurrent.CompletableFuture" in {
      val future = mock[MyCompletableFuture[String]]
      val result = underTest.supports(futureClass, future)

      result shouldEqual true
    }
    "does not support descendent classes of java.util.concurrent.CompletableFuture" in {
      val result = underTest.supports(classOf[MyCompletableFuture[_]], success)

      result shouldEqual false
    }
    "does not support non-CompletableFutures" in {
      val nonFuture = new Object()
      val result = underTest.supports(classOf[Object], nonFuture)

      result shouldEqual false
    }
    "does not support null class" in {
      val result = underTest.supports(null, success)

      result shouldEqual false
    }
    "does not support null" in {
      val result = underTest.supports(futureClass, null)

      result shouldEqual false
    }
    "calls whenComplete method on the future" in {
      val future = mock[CompletableFuture[String]]
      doReturn(future).when(future).whenComplete(any())

      val result = underTest.whenComplete(futureClass, future) { _ => {} }

      verify(future, times(1)).whenComplete(any())
      result shouldEqual future
    }
    "calls registered completion function for already completed future" in {
      val future = success
      val func = mock[Try[_] => Unit]

      underTest.whenComplete(futureClass, future)(func)

      verify(func, times(1)).apply(argEq(Try("success")))
    }
    "calls registered completion function for already exceptionally completed future" in {
      val ex = new RuntimeException
      val future = new CompletableFuture[String]()
      future.completeExceptionally(ex)
      val func = mock[Try[_] => Unit]
      val executionContext = new DirectExecutionContext()

      underTest.whenComplete(futureClass, future)(func)

      verify(func, times(1)).apply(argEq(Failure(ex)))
    }
    "calls registered completion function when the future completes successfully" in {
      val future = new CompletableFuture[String]()

      val func = mock[Try[_] => Unit]

      underTest.whenComplete(futureClass, future)(func)
      verify(func, never()).apply(any())

      future.complete("success")

      verify(func, times(1)).apply(argEq(Try("success")))
    }
    "calls registered completion function when the future completes exceptionally" in {
      val future = new CompletableFuture[String]()

      val func = mock[Try[_] => Unit]

      underTest.whenComplete(futureClass, future)(func)
      verify(func, never()).apply(any())

      val exception = new RuntimeException()
      future.completeExceptionally(exception)

      verify(func, times(1)).apply(argEq(Failure(exception)))
    }
  }

  private abstract class MyCompletableFuture[T] extends CompletableFuture[T] {}
}
