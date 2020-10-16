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

import java.util.concurrent.TimeUnit

import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

class NanoClockSpec extends AnyWordSpec with Matchers with MockitoSugar {

  "NanoClock" should {
    "get the epoch nanos from the System clock" in {
      val clock = mock[Clock]
      when(clock.now).thenReturn(1000000000L, 1200000000L, 1400000000L)
      when(clock.nanoTime).thenReturn(0L, 200000000L, 400000000L)

      val underTest = new NanoClock(clock, TimeUnit.MILLISECONDS.toNanos(50L))

      underTest.now shouldBe 1200000000L
      underTest.now shouldBe 1400000000L
    }

    "account for drift in the high-precision timers" in {
      val clock = mock[Clock]
      when(clock.now).thenReturn(1000000000L, 1200000000L, 1600000000L)
      when(clock.nanoTime).thenReturn(0L, 200000000L, 400000000L)

      val underTest = new NanoClock(clock, TimeUnit.MILLISECONDS.toNanos(50L))

      underTest.now shouldBe 1200000000L
      underTest.now shouldBe 1600000000L
    }

    "get the nanoTime from the underlying clock" in {
      val clock = mock[Clock]
      when(clock.nanoTime).thenReturn(0L, 200000000L, 400000000L)

      val underTest = new NanoClock(clock, TimeUnit.MILLISECONDS.toNanos(50L))

      underTest.nanoTime shouldBe 200000000L
      underTest.nanoTime shouldBe 400000000L
    }
  }
}
