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

package com.comcast.money.otel.formatters

import com.comcast.money.core.TraceGenerators
import com.comcast.money.core.formatters.FormatterUtils._
import com.comcast.money.otel.formatters.B3SingleHeaderFormatter.B3Header
import org.mockito.Mockito.{ verify, verifyNoMoreInteractions }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import scala.collection.mutable

class B3SingleHeaderFormatterSpec extends AnyWordSpec with MockitoSugar with Matchers with ScalaCheckDrivenPropertyChecks with TraceGenerators {
  val underTest = B3SingleHeaderFormatter
  val nullString = null.asInstanceOf[String]

  "B3SingleHeaderFormatter" should {
    "can roundtrip a span id" in {
      val spanId = randomRemoteSpanId()

      val map = mutable.Map[String, String]()

      underTest.toHttpHeaders(spanId, map.put)

      val result = underTest.fromHttpHeaders(Seq(), k => map.getOrElse(k, nullString))

      result shouldBe Some(spanId)
    }

    "lists the B3 headers" in {
      underTest.fields shouldBe Seq(B3Header)
    }

    "copy the request headers to the response" in {
      val setHeader = mock[(String, String) => Unit]

      underTest.setResponseHeaders({
        case B3Header => B3Header
      }, setHeader)

      verify(setHeader).apply(B3Header, B3Header)
      verifyNoMoreInteractions(setHeader)
    }
  }
}
