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

import com.comcast.money.core.SpecHelpers
import com.comcast.money.core.concurrent.ConcurrentSupport
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.{ any, eq => argEq }

import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.util.{ Failure, Try }
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.scalatest.OneInstancePerTest

class ScalaFutureNotificationHandlerSpec
  extends AnyWordSpec
  with MockitoSugar with Matchers with ConcurrentSupport with OneInstancePerTest with SpecHelpers {

  val success = Future.successful("success")
  val futureClass = classOf[Future[_]]
  val underTest = new ScalaFutureNotificationHandler()
  implicit val executionContext: ExecutionContext = new DirectExecutionContext()

  "ScalaFutureNotificationHandler" should {
    "support scala.concurrent.Future" in {
      val result = underTest.supports(futureClass, success)

      result shouldEqual true
    }
    "support descendents of scala.concurrent.Future" in {
      val future = mock[MyFuture[String]]
      val result = underTest.supports(futureClass, future)

      result shouldEqual true
    }
    "does not support descendent classes of scala.concurrent.Future" in {
      val result = underTest.supports(classOf[MyFuture[_]], success)

      result shouldEqual false
    }
    "does not support non-Future classes" in {
      val result = underTest.supports(classOf[Object], success)

      result shouldEqual false
    }
    "does not support non-Future instances" in {
      val result = underTest.supports(futureClass, new Object)

      result shouldEqual false
    }
    "does not support null classes" in {
      val result = underTest.supports(null, success)

      result shouldEqual false
    }
    "does not support null instances" in {
      val result = underTest.supports(futureClass, null)

      result shouldEqual false
    }
    "calls transform method on the future" in {
      val future = mock[Future[String]]
      val transformed = mock[Future[String]]
      when(future.transform(any(), any())(argEq(underTest.executionContext))).thenReturn(transformed.asInstanceOf[Future[Nothing]])

      val result = underTest.whenComplete(futureClass, future)(_ => {})

      result shouldBe transformed
      verify(future, times(1)).transform(any(), any())(argEq(underTest.executionContext))
    }
    "calls registered completion function for already completed future" in {
      val func = mock[Try[_] => Unit]

      underTest.whenComplete(futureClass, success)(func)

      verify(func, times(1)).apply(argEq(Try("success")))
    }
    "calls registered completion function for already exceptionally completed future" in {
      val ex = new RuntimeException
      val future = Future.failed(ex)
      val func = mock[Try[_] => Unit]

      underTest.whenComplete(futureClass, future)(func)

      verify(func, times(1)).apply(argEq(Failure(ex)))
    }
    "calls registered completion function when the future completes successfully" in {
      val promise = Promise[String]()
      val future = promise.future

      val func = mock[Try[_] => Unit]

      underTest.whenComplete(futureClass, future)(func)
      verify(func, never()).apply(any())

      promise.complete(Try("success"))

      verify(func, times(1)).apply(argEq(Try("success")))
    }
    "calls registered completion function when the future completes exceptionally" in {
      val promise = Promise[String]()
      val future = promise.future

      val func = mock[Try[_] => Unit]

      underTest.whenComplete(futureClass, future)(func)
      verify(func, never()).apply(any())

      val exception = new RuntimeException()
      promise.complete(Failure(exception))

      verify(func, times(1)).apply(argEq(Failure(exception)))
    }
  }

  private abstract class MyFuture[T] extends Future[T] {}
}
