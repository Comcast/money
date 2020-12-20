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

package com.comcast.money.core.internal

import com.comcast.money.api.SpanId
import org.slf4j.MDC

import scala.collection.JavaConverters._
import scala.collection.mutable
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{ BeforeAndAfterEach, OneInstancePerTest }
import org.scalatestplus.mockito.MockitoSugar
import com.comcast.money.api.Span
import com.comcast.money.api.SpanInfo
import org.mockito.Mockito._

class MDCSupportSpec extends AnyWordSpec with Matchers with MockitoSugar with BeforeAndAfterEach with OneInstancePerTest {

  val testMDCSupport = MDCSupport
  val spanId = SpanId.createNew()
  val span = mock[Span]
  val spanInfo = mock[SpanInfo]

  override def beforeEach() = {
    SpanLocal.clear()
    MDC.clear()
    when(span.info).thenReturn(spanInfo)
    when(spanInfo.id).thenReturn(spanId)
    when(spanInfo.name).thenReturn("spanName")
  }

  "MDCSupport" should {
    /*
    "set the span in MDC when provide" in {
      testMDCSupport.setSpanMDC(Some(span))
      MDC.get("moneyTrace") shouldEqual MDCSupport.format(spanId)
      MDC.get("spanName") shouldEqual "spanName"
    }
    "set the span in MDC in hex format" in {
      val mdcSupportFormatAsHex = new MDCSupport(true, true)
      mdcSupportFormatAsHex.setSpanMDC(Some(span))
      MDC.get("moneyTrace") shouldEqual MDCSupport.format(spanId, formatIdsAsHex = true)
      MDC.get("spanName") shouldEqual "spanName"
    }
    "clear the MDC value when set to None" in {
      testMDCSupport.setSpanMDC(Some(span))
      MDC.get("moneyTrace") shouldEqual MDCSupport.format(spanId)
      MDC.get("spanName") shouldEqual "spanName"

      testMDCSupport.setSpanMDC(None)
      MDC.get("moneyTrace") shouldBe null
      MDC.get("spanName") shouldBe null
    }
    "not be run if tracing is disabled" in {
      val disabled = new MDCSupport(false, false)
      disabled.setSpanMDC(Some(span))
      MDC.get("moneyTrace") shouldBe null
    }

     */
    "propagate MDC if not disabled" in {
      val mdcContext: mutable.Map[String, String] = mutable.HashMap("FINGERPRINT" -> "print")

      testMDCSupport.propagateMDC(Some(mdcContext.asJava))
      MDC.get("FINGERPRINT") shouldBe "print"
    }
    "clear MDC if given an empty context" in {
      MDC.put("FINGERPRINT", "print")
      testMDCSupport.propagateMDC(None)
      MDC.get("FINGERPRINT") shouldBe null
    }
  }
}
