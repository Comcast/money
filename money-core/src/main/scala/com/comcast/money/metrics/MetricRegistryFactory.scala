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

package com.comcast.money.metrics

import com.typesafe.config.Config
import akka.actor.{ ActorSystem }
import com.codahale.metrics.{ MetricRegistry, JmxReporter }
import scala.util.{ Try, Success, Failure }

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

  def metricRegistry(config: Config): MetricRegistry = {
    Try({
      // Try to create an instance of the custom factory, configured via 'metricRegistryFactory'
      val realFactory = Class.forName(config.getString("metricRegistryFactory"))
        .newInstance.asInstanceOf[MetricRegistryFactory]

      // Ask the custom factory for an MetricRegistry - and pass in our configuration so that an implementation
      // can add their settings in the application.conf, too.
      realFactory.metricRegistry(config)
    }) match {
      case Success(metricRegistry) => metricRegistry
      case Failure(ex) => {
        // Something went wrong while using the custom factory. Therefore creating the MetricRegistry
        // on our own and registering it to JMX (previous default behavior)
        new DefaultMetricRegistryFactory().metricRegistry(config)
      }
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
