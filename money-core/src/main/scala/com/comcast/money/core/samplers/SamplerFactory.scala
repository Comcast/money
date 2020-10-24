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

package com.comcast.money.core.samplers

import com.typesafe.config.Config
import org.slf4j.{ Logger, LoggerFactory }

object SamplerFactory {
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  def create(conf: Config): Sampler =
    conf.getString("type") match {
      case "always-on" => AlwaysOnSampler
      case "always-off" => AlwaysOffSampler
      case "percentage-based" => new PercentageBasedSampler(conf.getDouble("percentage"))
      case "custom" =>
        val className = conf.getString("class")
        val sampler = Class.forName(className).newInstance().asInstanceOf[Sampler]
        sampler match {
          case configurable: ConfigurableSampler => configurable.configure(conf)
          case _ =>
        }
        sampler
      case unknown =>
        logger.warn("Unknown sampler type: '{}'", unknown)
        AlwaysOnSampler
    }
}
