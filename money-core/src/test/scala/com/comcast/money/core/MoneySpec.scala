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

package com.comcast.money.core

import java.net.InetAddress

import com.comcast.money.core.handlers.AsyncSpanHandler
import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class MoneySpec extends AnyWordSpec with Matchers {

  val defaultConfig = ConfigFactory.load().getConfig("money")
  "Money" should {
    "load the reference config by default" in {
      val result = Money.Environment

      result.applicationName shouldBe "unknown"
      result.enabled shouldBe true
      result.factory shouldBe a[CoreSpanFactory]
      result.handler shouldBe an[AsyncSpanHandler]
      result.hostName shouldBe InetAddress.getLocalHost.getCanonicalHostName
      result.tracer should not be DisabledTracer
    }

    "load a Disabled Environment if money is disabled" in {
      val config = ConfigFactory.parseString(
        """
          |money {
          | enabled = false
          | application-name = "unknown"
          |}
        """.stripMargin)

      val result = Money(config.getConfig("money"))
      result.tracer shouldBe DisabledTracer
      result.factory shouldBe DisabledSpanFactory
      result.handler shouldBe DisabledSpanHandler
      result.enabled shouldBe false
    }
  }
}
