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

package com.comcast.money.core.metrics

import com.codahale.metrics.{ JmxReporter, MetricRegistry }
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

/*
 * Simple factory that tries to delegate to an implementation of this trait itself and announced via
 * the configuration 'metricRegistryFactory' - requiring that this implementation has a default constructor.
 *
 * It is up to the custom metricRegistryFactory what kind of MetricRegistry is created. No action is
 * performed on this MetricRegistry is performed (i.e. registering reporters) but used/passed back right away.
 *
 * If any error occurs (configuration is not set, class can't be loaded etc.) the default behavior
 * is performed and a fresh MetricRegistry is created and registered with the JmxReporter.
 *
 * Note: This trait should be kept as simple as possible so that the resulting interface can also be implemented
 * by a Java client custom factory.
 */
object MetricRegistryFactory {

  private val logger = LoggerFactory.getLogger("com.comcast.money.core.metrics.MetricRegistryFactory")

  def metricRegistry(config: Config): MetricRegistry = {
    try {
      val realFactory =
        if (config.hasPath("metrics-registry.class-name"))
          Class.forName(config.getString("metrics-registry.class-name")).newInstance.asInstanceOf[MetricRegistryFactory]
        else
          new DefaultMetricRegistryFactory()

      // Ask the custom factory for an MetricRegistry - and pass in our configuration so that an implementation
      // can add their settings in the application.conf, too.
      realFactory.metricRegistry(config)
    } catch {
      case e: Throwable =>
        logger.error("Unable to create actual factory instance", e)
        throw e
    }
  }
}

class DefaultMetricRegistryFactory extends MetricRegistryFactory {
  override def metricRegistry(config: Config): MetricRegistry = {
    val registry = new MetricRegistry
    val jmxReporter = JmxReporter.forRegistry(registry).build()
    jmxReporter.start()

    registry
  }
}

trait MetricRegistryFactory {
  def metricRegistry(config: Config): MetricRegistry
}
