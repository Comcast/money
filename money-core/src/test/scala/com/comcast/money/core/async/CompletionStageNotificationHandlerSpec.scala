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

class CompletionStageNotificationHandlerSpec
  extends WordSpecLike
  with MockitoSugar with Matchers with ConcurrentSupport with OneInstancePerTest with SpecHelpers {

  val underTest = new CompletionStageNotificationHandler()
  val futureClass: Class[_] = classOf[CompletionStage[_]]
  val success: CompletionStage[String] = CompletableFuture.completedFuture("success")

  "CompletionStageNotificationHandler" should {
    "support java.util.concurrent.CompletionStage" in {
      val result = underTest.supports(futureClass, success)

      result shouldEqual true
    }
    "support descendents of java.util.concurrent.CompletionStage" in {
      val stage = mock[MyCompletionStage[String]]
      val result = underTest.supports(futureClass, stage)

      result shouldEqual true
    }
    "does not support descendent classes of java.util.concurrent.CompletionStage" in {
      val result = underTest.supports(classOf[MyCompletionStage[_]], success)

      result shouldEqual false
    }
    "does not support non-CompletionStages" in {
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
    "calls whenComplete method on the completion stage" in {
      val stage = mock[CompletionStage[String]]
      doReturn(stage).when(stage).whenComplete(any())

      val result = underTest.whenComplete(futureClass, stage) { _ => {} }

      verify(stage, times(1)).whenComplete(any())
      result shouldEqual stage
    }
    "calls registered completion function for already completed stage" in {
      val stage = success
      val func = mock[Try[_] => Unit]

      underTest.whenComplete(futureClass, stage)(func)

      verify(func, times(1)).apply(argEq(Try("success")))
    }
    "calls registered completion function for already exceptionally completed stage" in {
      val ex = new RuntimeException
      val stage = new CompletableFuture[String]()
      stage.completeExceptionally(ex)
      val func = mock[Try[_] => Unit]
      val executionContext = new DirectExecutionContext()

      underTest.whenComplete(futureClass, stage)(func)

      verify(func, times(1)).apply(argEq(Failure(ex)))
    }
    "calls registered completion function when the stage completes successfully" in {
      val stage = new CompletableFuture[String]()

      val func = mock[Try[_] => Unit]

      underTest.whenComplete(futureClass, stage)(func)
      verify(func, never()).apply(any())

      stage.complete("success")

      verify(func, times(1)).apply(argEq(Try("success")))
    }
    "calls registered completion function when the stage completes exceptionally" in {
      val stage = new CompletableFuture[String]()

      val func = mock[Try[_] => Unit]

      underTest.whenComplete(futureClass, stage)(func)
      verify(func, never()).apply(any())

      val exception = new RuntimeException()
      stage.completeExceptionally(exception)

      verify(func, times(1)).apply(argEq(Failure(exception)))
    }
  }

  private abstract class MyCompletionStage[T] extends CompletionStage[T] {}
}
