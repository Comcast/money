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

package com.comcast.money.core.formatters

import com.comcast.money.core.DisabledFormatter
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

object FormatterFactory {
  private val logger = LoggerFactory.getLogger(getClass)

  def create(config: Config): Formatter =
    config.getString("type") match {
      case "trace-context" => new TraceContextFormatter()
      case "money-trace" => new MoneyTraceFormatter()
      case "ingress" => new IngressFormatter(FormatterChain(config))
      case "egress" => new EgressFormatter(FormatterChain(config))
      case "custom" =>
        val className = config.getString("class")
        Class.forName(className).newInstance() match {
          case configurable: ConfigurableFormatter =>
            configurable.configure(config)
            configurable
          case formatter: Formatter => formatter
        }
      case unknown =>
        logger.warn("Unknown formatter type: '{}'", unknown)
        DisabledFormatter
    }
}
