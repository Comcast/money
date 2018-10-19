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

package com.comcast.money.core.handlers

import com.comcast.money.api.{ SpanHandler, SpanInfo }
import com.comcast.money.core.SpecHelpers
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ Matchers, WordSpec }

class AsyncSpanHandlerSpec extends WordSpec with Matchers with MockitoSugar with TestData with SpecHelpers {

  class Wrapped extends SpanHandler {
    var called = false
    override def handle(span: SpanInfo): Unit = called = true
  }

  "AsyncSpanHandler" should {
    "asynchronously invoke the span handler" in {
      val spanHandler = new Wrapped()
      val underTest = new AsyncSpanHandler(scala.concurrent.ExecutionContext.global, spanHandler)

      underTest.handle(testSpanInfo)

      awaitCond(spanHandler.called)
    }
  }
}
