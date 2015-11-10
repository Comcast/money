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

package com.comcast.money.akka

import org.scalatest.WordSpec

import scala.annotation.tailrec
import scala.collection.mutable
import scala.concurrent.duration._
import scala.language.postfixOps

class BinaryExponentialBackOffSpec extends WordSpec {
  @tailrec
  final def nextBackOff(backOff: BinaryExponentialBackOff, cnt: Int): BinaryExponentialBackOff = {
    if (cnt == 0) {
      backOff
    } else {
      nextBackOff(backOff.nextBackOff, cnt - 1)
    }
  }

  def validBackOffs(slotTime: FiniteDuration, slot: Int): mutable.Set[FiniteDuration] = {
    val max = math.pow(2, slot) - 1
    val vals = mutable.TreeSet[FiniteDuration]()
    for (factor <- 0L to max.toLong) {
      vals += (slotTime * factor)
    }
    vals
  }

  "A BinaryExponential backoff " when {
    val backOff = BinaryExponentialBackOff(slotTime = 10 millis, ceiling = 10)

    "in initial state, next backoff" should {
      "be 0 or the slotTime" in {
        assert(!backOff.isStarted)
        for (a <- 1 to 100) {
          val newBackOff = backOff.nextBackOff
          assert(newBackOff.waitTime == Duration.Zero || newBackOff.waitTime.equals(backOff.slotTime))
        }
      }
    }
    "in slot 2" should {
      "be 0,1,2 or 3 x slotTime" in {
        for (a <- 1 to 400) {
          val newBackOff = backOff.nextBackOff.nextBackOff
          assert(
            newBackOff.waitTime == Duration.Zero || newBackOff.waitTime.equals(backOff.slotTime) || newBackOff.waitTime
              .equals(backOff.slotTime * 2) || newBackOff.waitTime.equals(backOff.slotTime * 3))
        }
      }
    }
    "in slot >=2 and slot < ceil" should {
      "be 0 to (2^slot) - 1" in {
        for (slot <- 3 to 9) {
          val validOnes = validBackOffs(backOff.slotTime, slot)
          for (repeat <- 1 to 1000) {
            val newBackOff = nextBackOff(backOff, slot)
            assert(validOnes.contains(newBackOff.waitTime))
            assert(newBackOff.isStarted)
          }
        }
      }
    }
    "in slot > ceil" should {
      "reset if stayAtCeil is false" in {
        val justBelowCeilBackOff = nextBackOff(backOff, 9)
        assert(justBelowCeilBackOff.isStarted)
        assert(justBelowCeilBackOff.resets == 0)
        assert(justBelowCeilBackOff.retries == 9)
        assert(justBelowCeilBackOff.totalRetries == 9)
        for (a <- 1 to 1000) {
          val validOnes = validBackOffs(backOff.slotTime, 1)
          val ceilBackoff = justBelowCeilBackOff.nextBackOff
          assert(!ceilBackoff.isStarted)
          assert(ceilBackoff.resets == 1)
          assert(ceilBackoff.retries == 0)
          assert(ceilBackoff.totalRetries == 9)
          assert(validOnes.contains(ceilBackoff.waitTime))
        }
      }
      "stay at ceil if stayAtCeil is true" in {
        val justBelowCeilBackOff = nextBackOff(backOff.copy(stayAtCeiling = true), 9)
        assert(justBelowCeilBackOff.isStarted)
        assert(justBelowCeilBackOff.resets == 0)
        assert(justBelowCeilBackOff.retries == 9)
        assert(justBelowCeilBackOff.totalRetries == 9)

        for (a <- 1 to 1000) {
          val validOnes = validBackOffs(backOff.slotTime, 10)
          val ceilBackoff = justBelowCeilBackOff.nextBackOff
          assert(ceilBackoff.isStarted)
          assert(ceilBackoff.resets == 0)
          assert(ceilBackoff.retries == 10)
          assert(ceilBackoff.totalRetries == 10)
          assert(validOnes.contains(ceilBackoff.waitTime))
        }
      }
    }
  }
}
