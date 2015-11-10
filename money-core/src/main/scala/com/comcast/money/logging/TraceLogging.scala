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

package com.comcast.money.logging

import com.comcast.money.core.Money
import org.slf4j.{LoggerFactory, Logger}

trait TraceLogging {

  lazy val shouldLogExceptions: Boolean = Money.logExceptions
  val logger: Logger = LoggerFactory.getLogger(classOf[TraceLogging])

  def logException(t: Throwable) = if (shouldLogExceptions) {
    logger.error("Tracing exception", t)
  }
}
