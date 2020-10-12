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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SystemClockSpec extends AnyWordSpec with Matchers {

  "SystemClock" should {
    "get the epoch nanos from the System clock" in {
      val before = System.currentTimeMillis * 1000000
      val rest = SystemClock.now
      val after = System.currentTimeMillis * 1000000

      rest should be >= before
      rest should be <= after
    }

    "get the nanoTime from the System clock" in {
      val before = System.nanoTime()
      val rest = SystemClock.nanoTime
      val after = System.nanoTime()

      rest should be >= before
      rest should be <= after
    }
  }
}
