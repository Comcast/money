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

package com.comcast.money.core.formatters

import com.comcast.money.core.DisabledFormatter
import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class FormatterFactorySpec extends AnyWordSpec with Matchers {
  "FormatterFactory" should {
    "return an instance of money-trace Formatter" in {
      val config = ConfigFactory.parseString("type = \"money-trace\"")

      val formatter = FormatterFactory.create(config)
      formatter shouldBe a[MoneyTraceFormatter]
    }

    "return an instance of trace-context Formatter" in {
      val config = ConfigFactory.parseString("type = \"trace-context\"")

      val formatter = FormatterFactory.create(config)
      formatter shouldBe a[TraceContextFormatter]
    }

    "return an instance of an ingress Formatter" in {
      val config = ConfigFactory.parseString(
        """
          |type = "ingress"
          |formatters = [
          | { type = "money-trace" },
          | { type = "trace-context" }
          |]
          |""".stripMargin)

      val formatter = FormatterFactory.create(config)
      formatter shouldBe a[IngressFormatter]
      val chain = formatter.asInstanceOf[IngressFormatter].formatter
      chain should matchPattern {
        case FormatterChain(Seq(_: MoneyTraceFormatter, _: TraceContextFormatter)) =>
      }
    }

    "return an instance of an egress Formatter" in {
      val config = ConfigFactory.parseString(
        """
          |type = "egress"
          |formatters = [
          | { type = "money-trace" },
          | { type = "trace-context" }
          |]
          |""".stripMargin)

      val formatter = FormatterFactory.create(config)
      formatter shouldBe a[EgressFormatter]
      val chain = formatter.asInstanceOf[EgressFormatter].formatter
      chain should matchPattern {
        case FormatterChain(Seq(_: MoneyTraceFormatter, _: TraceContextFormatter)) =>
      }
    }

    "create an instance of a custom Formatter" in {
      val config = ConfigFactory.parseString(
        s"""
           |type = "custom"
           |class = "${classOf[NonConfiguredFormatter].getCanonicalName}"
           |""".stripMargin)

      val formatter = FormatterFactory.create(config)
      formatter shouldBe a[NonConfiguredFormatter]
    }

    "create an instance of a custom configurable Formatter" in {
      val config = ConfigFactory.parseString(
        s"""
           |type = "custom"
           |class = "${classOf[ConfiguredFormatter].getCanonicalName}"
           |""".stripMargin)

      val formatter = FormatterFactory.create(config)
      formatter shouldBe a[ConfiguredFormatter]
      formatter.asInstanceOf[ConfiguredFormatter].calledConfigure shouldBe true
    }

    "returns a disabled formatter on an unknown type" in {
      val config = ConfigFactory.parseString("type = \"unknown\"")

      val formatter = FormatterFactory.create(config)
      formatter shouldBe DisabledFormatter
    }
  }
}
