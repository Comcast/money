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

package com.comcast.money.aspectj

import com.comcast.money.annotations.{Timed, Traced, TracedData}
import com.comcast.money.core.MoneyIntegrationSpec
import org.scalatest.GivenWhenThen

class TraceAspectIntegrationSpec extends MoneyIntegrationSpec with GivenWhenThen {

  @Traced("methodWithArguments")
  def methodWithArguments(@TracedData("foo") foo: String, @TracedData("CUSTOM_NAME") bar: String) = {
    Thread.sleep(50)
  }

  @Traced("methodWithoutArguments")
  def methodWithoutArguments() = {
    Thread.sleep(50)
  }

  @Traced("methodThrowingException")
  def methodThrowingException() = {
    Thread.sleep(50)
    throw new RuntimeException("test failure")
  }

  @Traced("methodThrowingExceptionWithNoMessage")
  def methodThrowingExceptionWithNoMessage() = {
    Thread.sleep(50)
    throw new RuntimeException()
  }

  @Traced("methodWithArgumentsPropagated")
  def methodWithArgumentsPropagated(@TracedData(value = "PROPAGATE", propagate = true) foo: String, @TracedData("CUSTOM_NAME") bar: String) = {
    Thread.sleep(50)
    methodWithoutArguments()
  }

  @Timed("methodWithTiming")
  def methodWithTiming() = {
    Thread.sleep(50)
  }

  "TraceAspect" when {
    "advising methods by tracing them" should {
      "handle methods that have no arguments" in {
        Given("a method that has the tracing annotation but has no arguments")
        When("the method is invoked")
        methodWithoutArguments()

        Then("the method execution is traced")
        expectSpanNamed("methodWithoutArguments")

        And("the result of success is captured")
        expectSpanResult(true)
      }
      "complete the trace for methods that throw exceptions" in {
        Given("a method that throws an exception")

        When("the method is invoked")
        a[RuntimeException] should be thrownBy {
          methodThrowingException()
        }

        Then("the method execution is logged")
        expectSpanNamed("methodThrowingException")

        And("a span-success is logged with a value of false")
        expectSpanResult(false)
      }
    }
    "advising methods that have parameters with the TracedData annotation" should {
      "record the value of the parameter in the trace" in {
        Given("a method that has arguments with the TraceData annotation")

        When("the method is invoked")
        methodWithArguments("hello", "bob")

        Then("The method execution is logged")
        expectSpanNamed("methodWithArguments")

        And("the values of the arguments that have the TracedData annotation are logged")
        expectSpanWithNote(_.getValue == "hello")

        And("the values of the arguments that have a custom name for the TracedData annotation log using the custom name")
        expectSpanWithNote(note => note.getName == "CUSTOM_NAME" && note.getValue == "bob")
      }
      "record parameters whose value is null" in {
        Given("a method that has arguments with the TraceData annotation")

        When("the method is invoked with a null value")
        methodWithArguments(null, null)

        Then("The method execution is logged")
        expectSpanNamed("methodWithArguments")

        And("the parameter values are captured")
        expectSpanWithNote(note => note.getName == "foo" && note.getValue == null)
        expectSpanWithNote(note => note.getName == "CUSTOM_NAME" && note.getValue == null)
      }
    }
  }
}
