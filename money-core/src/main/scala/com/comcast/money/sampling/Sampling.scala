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

package com.comcast.money.sampling

import com.comcast.money.core.Span
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

/**
 * Interface to be implemented by any sampler.  Determines if a span should be sampled or not.
 *
 * If this class is used in an Emitter, it is important to remember that Emitters are actor and therefore
 * are thread-safe.
 */
trait Sampler {
  /**
   * Method that needs to be implemented by a sampler.  Allows the sampler to set internal state prior to
   * sampling
   *
   * <pre><code>
   * sampling {
   *  enabled = true
   *  class-name = "com.comcast.money.sampling.EveryNSampler"
   *  every = 2
   * }
   * </code></pre>
   * @param config A configuration section containing the sampling config
   */
  def configure(config: Config): Unit

  /**
   * Determines if a span should be sampled
   *
   * @param span a Span to be evaluated
   * @return true if the span should be emitted; false otherwise
   */
  def sample(span: Span): Boolean
}

/**
 * Crude but effective sampler that samples every n spans.  Because the Emitters are actors, within a single
 * system this is thread safe
 *
 * This will sample every n spans.  If it is configured to 2, then every other span will be sampled.  If it is
 * configured to 10, then every 10th span will be sampled
 *
 * NOTE: this sampler will log ALL errors, sampling is only incorporated for successful spans
 *
 * @param n Value to determine the rate of span logging
 */
class EveryNSampler extends Sampler {

  private var everyN: Int = 1
  private var count: Int = 0

  def configure(config: Config): Unit = {
    everyN = config.getInt("every")
    ()
  }

  def sample(span: Span): Boolean =
    if (span.success) {
      count += 1
      if (count == everyN) {
        count = 0
        true
      } else {
        false
      }
    } else {
      // we ALWAYS record errors
      true
    }
}

object Sampling {

  private val logger = LoggerFactory.getLogger("com.comcast.money.sampling.Sampling")

  def sampler(config: Config): Sampler = {
    logger.info("creating logger with config...")
    logger.info(config.root().render())

    val enabled = config.hasPath("sampling") && config.getBoolean("sampling.enabled")

    if (enabled) {
      buildSampler(config.getConfig("sampling"))
    } else {
      logger.info("Sampling disabled!")
      new Sampler {
        // every span samples
        def sample(span: Span): Boolean = true
        def configure(config: Config): Unit = {}
      }
    }
  }

  private def buildSampler(config: Config): Sampler = {
    val className = config.getString("class-name")
    logger.info("Sampling enabled!  Using sampler {}", className)

    try {
      val sampler = Class.forName(className).newInstance().asInstanceOf[Sampler]
      sampler.configure(config)
      sampler
    } catch {
      case e: Throwable =>
        logger.error(s"Unable to build sampler, error encountered creating class '$className'", e)
        throw e
    }
  }
}
