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

package com.comcast.money.core

import _root_.akka.actor.ActorRef
import com.comcast.money.internal.EmitterProtocol.{EmitMetricLong, EmitMetricDouble}

/**
 * API to be used for emitting raw metrics
 */
trait Metrics {

  val emitterRef: ActorRef

  /**
   * Emits an individual metric.  This does not consider any trace context, but simply emits the metric value
   * @param path The path for the metric, or name for the metric
   * @param value The value for the metric being emitted
   */
  def sendMetric(path: String, value: Double): Unit = {
    emitterRef ! EmitMetricDouble(path, value)
  }

  /**
   * Emits an individual metric.  This does not consider any trace context, but simply emits the metric value
   * @param path The path for the metric, or name for the metric
   * @param value The value for the metric being emitted
   */
  def sendMetric(path: String, value: Long): Unit = {
    emitterRef ! EmitMetricLong(path, value)
  }
}
