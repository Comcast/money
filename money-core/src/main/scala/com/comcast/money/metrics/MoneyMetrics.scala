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

import java.util.concurrent.TimeUnit

import akka.actor.{ ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider }
import com.codahale.metrics.{ Timer, Counter, JmxReporter, MetricRegistry }

class MoneyMetricsImpl(activeSpans: Counter, timedOutSpans: Counter, spanDurations: Timer) extends Extension {

  def activateSpan() = activeSpans.inc()

  def stopSpan(duration: Long) = {
    activeSpans.dec()
    spanDurations.update(duration, TimeUnit.MILLISECONDS)
  }

  def stopSpanTimeout(duration: Long) = {
    stopSpan(duration)
    timedOutSpans.inc()
  }
}

object MoneyMetrics
    extends ExtensionId[MoneyMetricsImpl]
    with ExtensionIdProvider {

  val registry: MetricRegistry = new MetricRegistry()
  // register metrics
  val activeSpans = registry.counter("active.spans")
  val timedOutSpans = registry.counter("timed.out.spans")
  val spanDurations = registry.timer("span.duration")

  val jmxReporter = JmxReporter.forRegistry(registry).build()
  jmxReporter.start()

  //The lookup method is required by ExtensionIdProvider,
  // so we return ourselves here, this allows us
  // to configure our extension to be loaded when
  // the ActorSystem starts up
  override def lookup() = MoneyMetrics

  //This method will be called by Akka
  // to instantiate our Extension
  override def createExtension(system: ExtendedActorSystem) = new MoneyMetricsImpl(activeSpans, timedOutSpans, spanDurations)
}
