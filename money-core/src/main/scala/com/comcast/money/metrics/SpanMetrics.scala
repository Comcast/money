package com.comcast.money.metrics

import akka.actor.{Actor, ActorLogging, Props}
import com.codahale.metrics._
import com.comcast.money.akka.ActorMaker
import com.comcast.money.core.Span
import com.typesafe.config.Config

object SpanMetrics {

  val registry: MetricRegistry = new MetricRegistry

  JmxReporter.forRegistry(registry).build().start()

  def props(spanName: String) = {
    val latencyMetric: Histogram = registry.histogram(s"/money/$spanName:latency")
    val errorMetric: Meter = registry.meter(s"/money/$spanName:error")
    Props(classOf[SpanMetrics], spanName, latencyMetric, errorMetric)
  }
}

class SpanMetrics(spanName: String, latencyMetric: Histogram, errorMetric: Meter) extends Actor with ActorLogging {

  def receive = {
    case span: Span =>
      if (!span.success)
        errorMetric.mark()

      latencyMetric.update(span.duration)
  }
}

class SpanMetricsCollector(conf: Config) extends Actor with ActorMaker with ActorLogging {

  import com.comcast.money.internal.EmitterProtocol._

  def receive = {
    case EmitSpan(t: Span) =>
      context.child(t.spanName) match {
        case Some(spanMetrics) =>
          spanMetrics forward t
        case None =>
          val escapedName = t.spanName.replace(' ', '.')
          log.debug(s"Creating span metrics for span $escapedName")
          makeActor(SpanMetrics.props(escapedName), escapedName) forward t
      }
  }
}

