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

package com.comcast.money.core

import com.comcast.money.api.SpanId
import org.scalatest.{ Matchers, WordSpec }

class CoreSpanInfoSpec extends WordSpec with Matchers {

  "CoreSpanInfo" should {
    "have acceptable default values" in {
      val spanId = new SpanId()
      val underTest = CoreSpanInfo(spanId, "test")

      underTest.id shouldBe spanId
      underTest.name shouldBe "test"
      underTest.appName shouldBe Money.Environment.applicationName
      underTest.host shouldBe Money.Environment.hostName
      underTest.tags shouldBe empty
      underTest.success shouldBe true
      underTest.durationMicros shouldBe 0L
      underTest.startTimeMicros shouldBe 0L
      underTest.startTimeMillis shouldBe 0L
      underTest.endTimeMicros shouldBe 0L
      underTest.endTimeMillis shouldBe 0L
    }
  }
}
