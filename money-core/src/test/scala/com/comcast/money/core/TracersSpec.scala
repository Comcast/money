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

import com.comcast.money.core.Tracers._
import com.comcast.money.internal.SpanLocal
import com.comcast.money.util.DateTimeUtil
import org.mockito.Mockito._
import org.scalatest._
import org.scalatest.mock.MockitoSugar

import scala.collection.mutable

class TracersSpec extends FeatureSpec
with MockitoSugar
with GivenWhenThen
with BeforeAndAfterAll
with BeforeAndAfterEach
with OneInstancePerTest {

  val moneyTracer = mock[Tracer]

  override def beforeEach() = {
    val times = mutable.Stack(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L)
    DateTimeUtil.timeProvider = () => times.pop()
    SpanLocal.clear()
  }

  override def afterAll(): Unit = {
    DateTimeUtil.timeProvider = DateTimeUtil.SystemMicroTimeProvider
  }

  feature("Traced api") {
    scenario("Span is captured with no Span context") {
      When("a method is executed using the traced swag")
      traced("bob", moneyTracer) {
        Thread.sleep(1)
      }

      Then("Start and Stop span are called on the tracer")
      verify(moneyTracer).startSpan("bob")
      verify(moneyTracer).stopSpan(Note("span-success", true, 1L))
    }
    scenario("The function being traced throws an exception") {
      When("a method is executed using the traced swag and it throws an exception")
      intercept[RuntimeException] {
        traced("fail", moneyTracer) {
          throw new RuntimeException("we failed")
        }
      }

      Then("Start and Stop span are called on the tracer")
      verify(moneyTracer).startSpan("fail")

      And("the stop span is called with a failure result")
      verify(moneyTracer).stopSpan(Note("span-success", false, 1L))
    }
  }
  feature("Timed api") {
    scenario("Trace context exists") {
      When("a method is executed using the timed swag")
      SpanLocal.push(SpanId())
      timed("bob", moneyTracer) {
        Thread.sleep(1)
      }

      Then("Start and Stop timer are called on the tracer")
      verify(moneyTracer).startTimer("bob")
      verify(moneyTracer).stopTimer("bob")
    }
    scenario("The function being timed throws an exception") {
      When("a method is executed using the timed swag and it throws an exception")
      SpanLocal.push(SpanId())
      intercept[RuntimeException] {
        timed("fail", moneyTracer) {
          throw new RuntimeException("we failed")
        }
      }

      Then("Start and Stop span are called on the tracer")
      verify(moneyTracer).startTimer("fail")

      And("start and stop timer are called on the tracer")
      verify(moneyTracer).stopTimer("fail")
    }
  }
}
