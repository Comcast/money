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
import com.typesafe.config.ConfigFactory
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.{ Matchers, OneInstancePerTest, WordSpecLike }
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.Future

class AsyncNotifierSpec
  extends WordSpecLike
  with MockitoSugar with Matchers with ConcurrentSupport with OneInstancePerTest with SpecHelpers {

  "AsyncNotifier" should {
    "create a sequence of AsyncNotificationServices" in {
      val config = ConfigFactory.parseString(
        """
          |{
          | handlers = [
          |   {
          |     class = "com.comcast.money.core.async.NonConfiguredNotificationHandler"
          |   },
          |   {
          |     class = "com.comcast.money.core.async.NonConfiguredNotificationHandler"
          |   }
          | ]
          |}
        """.stripMargin)

      val result = AsyncNotifier(config)
      result shouldBe a[AsyncNotifier]
      result.handlers.size shouldBe 2
    }
    "configures an AsyncNotificationHandler that implements ConfigurableNotificationHandler" in {
      val config = ConfigFactory.parseString(
        """
          |{
          | handlers = [
          |   {
          |     class = "com.comcast.money.core.async.NonConfiguredNotificationHandler"
          |   },
          |   {
          |     class = "com.comcast.money.core.async.ConfiguredNotificationHandler"
          |   }
          | ]
          |}
        """.stripMargin)

      val result = AsyncNotifier(config)
      result shouldBe a[AsyncNotifier]
      result.handlers.size shouldBe 2

      result.handlers.head shouldBe a[NonConfiguredNotificationHandler]
      result.handlers.last shouldBe a[ConfiguredNotificationHandler]

      result.handlers.last.asInstanceOf[ConfiguredNotificationHandler].calledConfigure shouldBe true
    }
    "find AsyncNotificationHandler that supports Future" in {
      val mockHandler = mock[AsyncNotificationHandler]
      val futureClass = classOf[Future[_]]
      val future = mock[Future[String]]

      val asyncNotifier = AsyncNotifier(Seq(mockHandler))
      doReturn(true).when(mockHandler).supports(futureClass, future)

      val result = asyncNotifier.resolveHandler(futureClass, future)

      verify(mockHandler, times(1)).supports(futureClass, future)

      result.isDefined shouldEqual true
      result.get shouldEqual mockHandler
    }
    "not find any AsyncNotificationHandler for null futureClass" in {
      val mockHandler = mock[AsyncNotificationHandler]
      val future = mock[Future[String]]

      val asyncNotifier = AsyncNotifier(Seq(mockHandler))
      doReturn(true).when(mockHandler).supports(any[Class[_]], any)

      val result = asyncNotifier.resolveHandler(null, future)

      result.isEmpty shouldEqual true
      verify(mockHandler, never).supports(any[Class[_]], any)
    }
    "not find any AsyncNotificationHandler for null future" in {
      val mockHandler = mock[AsyncNotificationHandler]

      val asyncNotifier = AsyncNotifier(Seq(mockHandler))
      doReturn(true).when(mockHandler).supports(any[Class[_]], any)

      val result = asyncNotifier.resolveHandler(classOf[Future[_]], null)

      result.isEmpty shouldEqual true
      verify(mockHandler, never).supports(any[Class[_]], any)
    }
    "not find AsyncNotificationHandler when not supported" in {
      val mockHandler = mock[AsyncNotificationHandler]

      val asyncNotifier = AsyncNotifier(Seq(mockHandler))
      doReturn(false).when(mockHandler).supports(any[Class[_]], any)

      val result = asyncNotifier.resolveHandler(classOf[Future[_]], new Object)

      result.isEmpty shouldEqual true
      verify(mockHandler, times(1)).supports(any[Class[_]], any)
    }
  }
}
