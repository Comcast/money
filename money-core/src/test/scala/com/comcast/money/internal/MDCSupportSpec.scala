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

package com.comcast.money.internal

import com.comcast.money.core.SpanId
import org.scalatest.{BeforeAndAfterEach, Matchers, OneInstancePerTest, WordSpec}
import org.slf4j.MDC

import scala.collection.mutable

class MDCSupportSpec extends WordSpec with Matchers with BeforeAndAfterEach with OneInstancePerTest {

  val testMDCSupport = new MDCSupport
  val spanId = SpanId()

  override def beforeEach() = {
    SpanLocal.clear()
  }

  "MDCSupport" should {
    "set the span in MDC when provide" in {
      testMDCSupport.setSpanMDC(Some(spanId))
      MDC.get("moneyTrace") shouldEqual MDCSupport.format(spanId)
    }
    "clear the MDC value when set to None" in {
      testMDCSupport.setSpanMDC(Some(spanId))
      MDC.get("moneyTrace") shouldEqual MDCSupport.format(spanId)

      testMDCSupport.setSpanMDC(None)
      MDC.get("moneyTrace") shouldBe null
    }
    "not be run if tracing is disabled" in {
      val disabled = new MDCSupport(false)
      disabled.setSpanMDC(Some(spanId))
      MDC.get("moneyTrace") shouldBe null
    }
    "not propogate MDC if disabled" in {
      import scala.collection.JavaConversions._

      val mdcContext: mutable.Map[_, _] = mutable.HashMap("FINGERPRINT" -> "print")
      val disabled = new MDCSupport(false)
      disabled.propogateMDC(Some(mdcContext))
      MDC.get("FINGERPRINT") shouldBe null
    }
    "propogate MDC if not disabled" in {
      import scala.collection.JavaConversions._

      val mdcContext: mutable.Map[_, _] = mutable.HashMap("FINGERPRINT" -> "print")

      testMDCSupport.propogateMDC(Some(mdcContext))
      MDC.get("FINGERPRINT") shouldBe "print"
    }
    "clear MDC if given an empty context" in {
      MDC.put("FINGERPRINT", "print")
      testMDCSupport.propogateMDC(None)
      MDC.get("FINGERPRINT") shouldBe null
    }
    "set span name" in {
      testMDCSupport.setSpanNameMDC(Some("foo"))
      MDC.get("spanName") shouldBe "foo"
      testMDCSupport.getSpanNameMDC shouldBe Some("foo")
    }
    "clear span name from MDC when given an empty value" in {
      MDC.put("spanName", "shouldBeRemoved")
      testMDCSupport.setSpanNameMDC(None)
      MDC.get("spanName") shouldBe null
      testMDCSupport.getSpanNameMDC shouldBe None
    }
  }
}
