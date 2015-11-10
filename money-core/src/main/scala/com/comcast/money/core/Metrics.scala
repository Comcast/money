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
