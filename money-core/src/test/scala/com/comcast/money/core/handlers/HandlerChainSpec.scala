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

package com.comcast.money.core.handlers

import com.comcast.money.api.SpanHandler
import com.typesafe.config.ConfigFactory
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ Matchers, WordSpec }

class HandlerChainSpec extends WordSpec with Matchers with MockitoSugar with TestData {

  "HandlerChain" should {

    "invoke all handlers in the chain in order" in {
      val handler1 = mock[SpanHandler]
      val handler2 = mock[SpanHandler]
      val handler3 = mock[SpanHandler]

      val ordered = Mockito.inOrder(handler1, handler2, handler3)

      val underTest = HandlerChain(Seq(handler1, handler2, handler3))

      underTest.handle(testSpanInfo)

      ordered.verify(handler1).handle(testSpanInfo)
      ordered.verify(handler2).handle(testSpanInfo)
      ordered.verify(handler3).handle(testSpanInfo)
    }

    "continues invocation of chain if one of the handlers throws an exception" in {

      val handler1 = mock[SpanHandler]
      val handler2 = mock[SpanHandler]
      val handler3 = mock[SpanHandler]

      doThrow(classOf[RuntimeException]).when(handler1).handle(testSpanInfo)
      val ordered = Mockito.inOrder(handler1, handler2, handler3)

      val underTest = HandlerChain(Seq(handler1, handler2, handler3))

      underTest.handle(testSpanInfo)

      ordered.verify(handler1).handle(testSpanInfo)
      ordered.verify(handler2).handle(testSpanInfo)
      ordered.verify(handler3).handle(testSpanInfo)
    }

    "create a sequence of handlers" in {
      val config = ConfigFactory.parseString(
        """
          |{
          | async = false
          | handlers = [
          |   {
          |     class = "com.comcast.money.core.handlers.ConfiguredHandler"
          |   },
          |   {
          |     class = "com.comcast.money.core.handlers.ConfiguredHandler"
          |   },
          |   {
          |     class = "com.comcast.money.core.handlers.NonConfiguredHandler"
          |   }
          | ]
          |}
        """.stripMargin
      )

      val result = HandlerChain(config)

      result shouldBe a[HandlerChain]
      result.asInstanceOf[HandlerChain].handlers should have size 3
    }

    "wrap the handler chain in an async handler if async is set to true" in {
      val config = ConfigFactory.parseString(
        """
          |{
          | async = true
          | handlers = [
          |   {
          |     class = "com.comcast.money.core.handlers.ConfiguredHandler"
          |   },
          |   {
          |     class = "com.comcast.money.core.handlers.ConfiguredHandler"
          |   },
          |   {
          |     class = "com.comcast.money.core.handlers.NonConfiguredHandler"
          |   }
          | ]
          |}
        """.stripMargin
      )

      val result = HandlerChain(config)

      result shouldBe an[AsyncSpanHandler]
      result.asInstanceOf[AsyncSpanHandler]
        .wrapped.asInstanceOf[HandlerChain]
        .handlers should have size 3
    }
  }
}
