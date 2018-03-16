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
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.{ Matchers, OneInstancePerTest, WordSpecLike }
import org.scalatest.mock.MockitoSugar

import scala.concurrent.Future

class AsyncNotificationServiceSpec
    extends WordSpecLike
    with MockitoSugar with Matchers with ConcurrentSupport with OneInstancePerTest with SpecHelpers {

  "AsyncNotificationService" should {
    "find ScalaFutureNotificationService for a ScalaFuture" in {
      val future = mock[Future[String]]
      val service = AsyncNotificationService.findNotificationService(future)

      service.isDefined shouldEqual true
      service.get shouldBe a[ScalaFutureNotificationService]
    }
    "not find any service for null" in {
      val service = AsyncNotificationService.findNotificationService(null)

      service.isEmpty shouldEqual true
    }
    "not find any service for Object" in {
      val obj = new Object
      val service = AsyncNotificationService.findNotificationService(obj)

      service.isEmpty shouldEqual true
    }
    "support registration of completion for TestFuture with successful result" in {
      val future = mock[Future[String]]
      val service = AsyncNotificationService.findNotificationService(future)

      val func = mock[(Any, Throwable) => Unit]
      service.get.whenDone(future, func)

      val captor = ArgumentCaptor.forClass(classOf[String => String])
      verify(future, times(1)).transform(captor.capture(), any())(any())

      val captured = captor.getValue
      captured.apply("success")

      verify(func, times(1)).apply("success", null)
    }
    "support registration of completion for TestFuture with failure result" in {
      val future = mock[Future[String]]
      val service = AsyncNotificationService.findNotificationService(future)

      val func = mock[(Any, Throwable) => Unit]
      service.get.whenDone(future, func)

      val captor = ArgumentCaptor.forClass(classOf[Throwable => Throwable])
      verify(future, times(1)).transform(any(), captor.capture())(any())

      val captured = captor.getValue
      val exception = new RuntimeException
      captured.apply(exception)

      verify(func, times(1)).apply(null, exception)
    }
  }
}
