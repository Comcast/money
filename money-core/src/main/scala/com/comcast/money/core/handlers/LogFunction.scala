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

package com.comcast.money.core.handlers

import com.typesafe.config.Config
import org.slf4j.Logger

private[core] object LogFunction {
  val LOG_LEVEL_KEY: String = "log-level"

  def apply(logger: Logger, config: Config): String => Unit =
    if (config.hasPath(LOG_LEVEL_KEY)) {
      config.getString(LOG_LEVEL_KEY).toUpperCase match {
        case "ERROR" => logger.error
        case "WARN" => logger.warn
        case "INFO" => logger.info
        case "DEBUG" => logger.debug
        case "TRACE" => logger.trace
      }
    } else {
      logger.info
    }
}
