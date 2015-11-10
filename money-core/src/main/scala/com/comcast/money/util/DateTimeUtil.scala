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

package com.comcast.money.util

/**
 * Used to look up the current system time in microseconds
 */
object DateTimeUtil {

  /**
   * Produces the current time in microseconds
   */
  type MicroTimeProvider = () => Long

  /**
   * The default time provider that uses current system time
   */
  val SystemMicroTimeProvider: MicroTimeProvider = () => System.nanoTime() / 1000L

  /**
   * Allows us to override the time provider, useful for testing purposes
   *
   */
  var timeProvider: MicroTimeProvider = SystemMicroTimeProvider

  /**
   * @return The current time UTC in microseconds
   */
  def microTime: Long = timeProvider()
}
