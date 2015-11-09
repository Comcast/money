package com.comcast.money.metrics

import java.util.concurrent.TimeUnit

import akka.actor.ExtendedActorSystem
import com.codahale.metrics._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{OneInstancePerTest, Matchers, WordSpec}

class MoneyMetricsSpec extends WordSpec with Matchers with MockitoSugar with OneInstancePerTest {

  private val activeSpans = mock[Counter]
  private val timedOutSpans = mock[Counter]
  private val spanDurations = mock[Timer]
  private val underTest = new MoneyMetricsImpl(activeSpans, timedOutSpans, spanDurations)

  "MoneyMetrics" should {
    "increment active spans" in {
      underTest.activateSpan()
      verify(activeSpans).inc()
    }
    "decrement active spans and update span durations when stopping a span" in {
      underTest.stopSpan(1000)
      verify(activeSpans).dec()
      verify(spanDurations).update(1000, TimeUnit.MILLISECONDS)
    }
    "decrement active spans, update span durations, and update timed out counter" in {
      underTest.stopSpanTimeout(1000)
      verify(activeSpans).dec()
      verify(spanDurations).update(1000, TimeUnit.MILLISECONDS)
      verify(timedOutSpans).inc()
    }
  }

  "MoneyMetrics Extension" should {
    "initialize all metrics" in {
      val eas = mock[ExtendedActorSystem]
      val mm = MoneyMetrics.lookup().createExtension(eas)

      MoneyMetrics.registry.getCounters.get("active.spans") shouldNot be(null)
      MoneyMetrics.registry.getCounters.get("timed.out.spans") shouldNot be(null)
      MoneyMetrics.registry.getTimers.get("span.duration") shouldNot be(null)
    }
  }
}

