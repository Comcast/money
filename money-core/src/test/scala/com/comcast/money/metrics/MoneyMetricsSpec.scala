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

import akka.actor.ExtendedActorSystem
import com.codahale.metrics._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ OneInstancePerTest, Matchers, WordSpec }

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
}
