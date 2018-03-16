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

package com.comcast.money.core.async

import com.comcast.money.core.SpecHelpers
import com.comcast.money.core.concurrent.ConcurrentSupport
import org.scalatest.{ Matchers, OneInstancePerTest, WordSpecLike }
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers.{ any, argThat, eq => argEq }
import org.hamcrest.CoreMatchers.nullValue

import scala.concurrent.{ Await, ExecutionContext, Future, Promise }
import scala.concurrent.duration._
import scala.util.{ Failure, Try }

class ScalaFutureNotificationServiceSpec
    extends WordSpecLike
    with MockitoSugar with Matchers with ConcurrentSupport with OneInstancePerTest with SpecHelpers {

  val underTest = new ScalaFutureNotificationService()
  implicit val ec: ExecutionContext = ExecutionContext.global

  "ScalaFutureTracingService" should {
    "support scala.concurrent.Future" in {
      val future = Future("success")
      val result = underTest.supports(future)

      result shouldEqual true
    }
    "support descendents of scala.concurrent.Future" in {
      val future = mock[MyFuture[String]]
      val result = underTest.supports(future)

      result shouldEqual true
    }
    "does not support non-Futures" in {
      val nonFuture = new Object()
      val result = underTest.supports(nonFuture)

      result shouldEqual false
    }
    "does not support null" in {
      val result = underTest.supports(null)

      result shouldEqual false
    }
    "calls transform method on the future" in {
      val future = mock[Future[String]]
      doReturn(future).when(future).transform(any(), any())(any())

      val result = underTest.whenDone(future, (_, _) => {})

      verify(result, times(1)).transform(any(), any())(any())
    }
    "calls registered completion function for already completed future" in {
      val future = Future("success")
      val func = mock[(Any, Throwable) => Unit]

      val result = underTest.whenDone(future, func)

    }
    "calls registered completion function when the future completes successfully" in {
      val promise = Promise[String]()
      val future = promise.future

      val func = mock[(Any, Throwable) => Unit]

      val result = underTest.whenDone(future, func)
      verify(func, never()).apply(any(), any())

      promise.complete(Try("success"))
      Await.ready(result.asInstanceOf[Future[String]], 50 millis)

      verify(func, times(1)).apply(argEq("success"), argThat(nullValue[Throwable]()))
    }
    "calls registered completion function when the future completes exceptionally" in {
      val promise = Promise[String]()
      val future = promise.future

      val func = mock[(Any, Throwable) => Unit]

      val result = underTest.whenDone(future, func)
      verify(func, never()).apply(any(), any())

      val exception = new RuntimeException()
      promise.complete(Failure(exception))
      Await.ready(result.asInstanceOf[Future[String]], 50 millis)

      verify(func, times(1)).apply(argThat(nullValue[String]), argEq(exception))
    }
  }

  private abstract class MyFuture[T] extends Future[T] {}
}
