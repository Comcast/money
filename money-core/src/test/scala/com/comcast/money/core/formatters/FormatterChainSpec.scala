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

import com.comcast.money.api.SpanId
import com.typesafe.config.ConfigFactory
import org.mockito.Mockito
import org.mockito.Mockito.{ verify, verifyZeroInteractions, when }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

class FormatterChainSpec extends AnyWordSpec with MockitoSugar with Matchers {
  private val formatter1 = mock[Formatter]
  private val formatter2 = mock[Formatter]

  private val underTest = FormatterChain(Seq(formatter1, formatter2))

  "FormatterChain" should {
    "return the default formatters" in {
      val result = FormatterChain.default

      result.formatters should have size 2
      val Seq(formatter1, formatter2) = result.formatters
      formatter1 shouldBe a[MoneyTraceFormatter]
      formatter2 shouldBe a[TraceContextFormatter]
    }

    "return configured formatters" in {
      val config = ConfigFactory.parseString(
        """
          | formatters = [
          |   {
          |     type = custom
          |     class = com.comcast.money.core.formatters.TraceContextFormatter
          |   },
          |   {
          |     type = custom
          |     class = com.comcast.money.core.formatters.MoneyTraceFormatter
          |   }
          | ]
          |""".stripMargin)

      val result = FormatterChain(config)

      val Seq(formatter1, formatter2) = result.formatters
      formatter1 shouldBe a[TraceContextFormatter]
      formatter2 shouldBe a[MoneyTraceFormatter]
    }

    "calls toHttpHeaders on all Formatters" in {
      val spanId = SpanId.createNew()
      val setter = mock[(String, String) => Unit]
      underTest.toHttpHeaders(spanId, setter)

      val inOrder = Mockito.inOrder(formatter1, formatter2)
      inOrder.verify(formatter1).toHttpHeaders(spanId, setter)
      inOrder.verify(formatter2).toHttpHeaders(spanId, setter)
    }

    "attempts fromHttpHeaders on all Formatters" in {
      val getter = mock[String => String]
      val log = mock[String => Unit]

      when(formatter1.fromHttpHeaders(getter, log)).thenReturn(None)
      when(formatter2.fromHttpHeaders(getter, log)).thenReturn(None)

      val result = underTest.fromHttpHeaders(getter, log)

      result shouldBe None

      val inOrder = Mockito.inOrder(formatter1, formatter2)
      inOrder.verify(formatter1).fromHttpHeaders(getter, log)
      inOrder.verify(formatter2).fromHttpHeaders(getter, log)
    }

    "returns the first SpanId in the chain" in {
      val spanId = SpanId.createNew()
      val getter = mock[String => String]
      val log = mock[String => Unit]

      when(formatter1.fromHttpHeaders(getter, log)).thenReturn(Some(spanId))
      when(formatter2.fromHttpHeaders(getter, log)).thenReturn(None)

      val result = underTest.fromHttpHeaders(getter, log)

      result shouldBe Some(spanId)

      verify(formatter1).fromHttpHeaders(getter, log)
      verifyZeroInteractions(formatter2)
    }

    "returns the combined fields from each Formatter in the chain" in {
      when(formatter1.fields).thenReturn(Seq("A", "B"))
      when(formatter2.fields).thenReturn(Seq("C", "D"))

      underTest.fields shouldBe Seq("A", "B", "C", "D")
    }

    "sets the response headers for each Formatter" in {
      val getter = mock[String => String]
      val setter = mock[(String, String) => Unit]

      underTest.setResponseHeaders(getter, setter)

      val inOrder = Mockito.inOrder(formatter1, formatter2)
      inOrder.verify(formatter1).setResponseHeaders(getter, setter)
      inOrder.verify(formatter2).setResponseHeaders(getter, setter)
    }
  }
}
