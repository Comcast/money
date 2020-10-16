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

import java.util.concurrent.atomic.AtomicReference

class NanoClock(clock: Clock, maxDriftNanos: Long) extends Clock {

  private val initialTimestamp = new AtomicReference[(Long, Long)]((clock.now, clock.nanoTime))

  override def now: Long = {

    val initialTime = initialTimestamp.get
    val (initialNow, initialNanoTime) = initialTime

    val currentNanoTime = clock.nanoTime
    val deltaNanos = currentNanoTime - initialNanoTime

    val currentNow = clock.now
    val deltaNow = currentNow - initialNow

    if (Math.abs(deltaNanos - deltaNow) > maxDriftNanos) {
      if (initialTimestamp.compareAndSet(initialTime, (currentNow, currentNanoTime))) {
        return currentNow
      }
    }
    initialNow + deltaNanos
  }

  override def nanoTime: Long = clock.nanoTime
}
