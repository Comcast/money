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

import akka.testkit.TestActorRef
import com.codahale.metrics.{ Histogram, Meter }
import com.comcast.money.api.SpanId
import com.comcast.money.core.{ LongNote, Note, Span }
import com.comcast.money.internal.EmitterProtocol.EmitSpan
import com.comcast.money.test.AkkaTestJawn
import com.typesafe.config.Config
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ FeatureSpecLike, GivenWhenThen, Matchers }

class SpanMetricsSpec extends AkkaTestJawn with FeatureSpecLike with Matchers with GivenWhenThen with MockitoSugar {

  val conf = mock[Config]
  doReturn("com.comcast.money.metrics.MockMetricRegistryFactory").when(conf).getString("metrics-registry.class-name")

  feature("Span Metrics Collector") {
    scenario("A span arrives for a new span name that we haven't seen yet") {
      Given("a span metrics collector is registered")
      val collector = TestActorRef(new SpanMetricsCollector(conf) with TestProbeMaker)

      When("a span arrives that we haven't seen yet")
      val span = Span(
        new SpanId("foo", 1L), "happy.span", "app", "host", 1L, true, 35L, Map(
          "when" -> Note("when", 1.5), "who" -> LongNote("who", None, 45), "bob" -> Note("bob", "1.2"),
          "apple" -> Note("apple", "pie")
        )
      )
      collector ! EmitSpan(span)

      Then("a new span metrics child actor is created")
      And("the span is forwarded to the new child")
      child(collector, "happy.span").expectMsg(span)
    }

    scenario("A span arrives for a span name that we have seen already") {
      Given("a span metrics instance already exists")
      val collector = TestActorRef(new SpanMetricsCollector(conf) with TestProbeMaker)
      val span = Span(
        new SpanId("foo", 1L), "happy.span", "app", "host", 1L, true, 35L, Map(
          "when" -> Note("when", 1.5), "who" -> LongNote("who", None, 45), "duration" -> Note("duration", 3.5),
          "bob" -> Note("bob", "1.2"), "apple" -> Note("apple", "pie")
        )
      )
      collector ! EmitSpan(span)
      child(collector, "happy.span").expectMsg(span)

      When("another span arrives with the same name")
      val span2 = span.copy()
      collector ! EmitSpan(span2)

      Then("the message is forwarded to the existing child")
      child(collector, "happy.span").expectMsg(span2)
    }
  }

  feature("Collecting metrics for a span") {
    scenario("the span is not an error") {
      // TODO: Test Fixture This!
      val latencyMetric = mock[Histogram]
      val errorMetric = mock[Meter]
      val span = Span(new SpanId("foo", 1L), "test.span", "app", "host", 1L, true, 200L, Map())

      When("the span metrics is received")
      val spanMetrics = TestActorRef(new SpanMetrics("test.span", latencyMetric, errorMetric))
      spanMetrics ! span

      Then("the latency metric is incremented")
      verify(latencyMetric).update(200L)

      And("the error metric is not updated")
      verifyZeroInteractions(errorMetric)
    }
    scenario("the span is an error") {
      val latencyMetric = mock[Histogram]
      val errorMetric = mock[Meter]
      val span = Span(
        new SpanId("foo", 1L), "test.span", "app", "host", 1L, false, 200L, Map(
          "span-success" -> Note("span-success", false), "span-duration" -> Note("span-duration", 200.0)
        )
      )

      When("the span metrics is received")
      val spanMetrics = TestActorRef(new SpanMetrics("test.span", latencyMetric, errorMetric))
      spanMetrics ! span

      Then("the latency metric is incremented")
      verify(latencyMetric).update(200L)

      And("the error metric is also updated")
      verify(errorMetric).mark()
    }
    scenario("the result is not present") {
      val latencyMetric = mock[Histogram]
      val errorMetric = mock[Meter]
      val span = Span(
        new SpanId("foo", 1L), "test.span", "app", "host", 1L, true, 200L, Map("span-duration" -> Note("span-duration", 200.0))
      )

      When("the span metrics is received")
      val spanMetrics = TestActorRef(new SpanMetrics("test.span", latencyMetric, errorMetric))
      spanMetrics ! span

      Then("the latency metric is incremented")
      verify(latencyMetric).update(200L)

      And("the error metric is not updated")
      verifyZeroInteractions(errorMetric)
    }
  }
}
