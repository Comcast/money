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

package com.comcast.money.features

import java.lang.{Class => JavaClass}

import akka.testkit.TestKit.{awaitCond => testKitAwaitCond}
import com.comcast.money.api.SpanId
import com.comcast.money.core.Money
import com.comcast.money.emitters.GraphiteMetricEmitter._
import com.comcast.money.emitters.LogRecord
import com.comcast.money.graphite.TestGraphiteServer
import com.comcast.money.internal.SpanLocal
import com.comcast.money.util.DateTimeUtil
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTimeUtils
import org.joda.time.DateTimeUtils.MillisProvider
import org.scalatest._

import scala.collection.JavaConversions._
import scala.concurrent._
import scala.concurrent.duration._

class TracerIntegrationSpec extends FeatureSpec with Matchers with GivenWhenThen with BeforeAndAfter with Inspectors {

  setEnv(Map("MONEY_ENV" -> "test"))

  val moneyTracer = Money.tracer
  val defaultConfig = ConfigFactory.load()

  /**
   * horrible hack to stage the in memory representation of the
   * Environment variables so sys.
   *
   * @param newenv
   */
  def setEnv(newenv: java.util.Map[String, String]): Unit = {
    try {
      val processEnvironmentClass = JavaClass.forName("java.lang.ProcessEnvironment")
      val theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment")
      theEnvironmentField.setAccessible(true)

      val variableClass = JavaClass.forName("java.lang.ProcessEnvironment$Variable")
      val convertToVariable = variableClass.getMethod("valueOf", classOf[java.lang.String])
      convertToVariable.setAccessible(true)

      val valueClass = JavaClass.forName("java.lang.ProcessEnvironment$Value")
      val convertToValue = valueClass.getMethod("valueOf", classOf[java.lang.String])
      convertToValue.setAccessible(true)

      val sampleVariable = convertToVariable.invoke(null, "")
      val sampleValue = convertToValue.invoke(null, "")
      val env = theEnvironmentField.get(null).asInstanceOf[java.util.Map[sampleVariable.type, sampleValue.type]]
      newenv.foreach { case (k, v) => {
        val variable = convertToVariable.invoke(null, k).asInstanceOf[sampleVariable.type]
        val value = convertToValue.invoke(null, v).asInstanceOf[sampleValue.type]
        env.put(variable, value)
      }
      }

      val theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment")
      theCaseInsensitiveEnvironmentField.setAccessible(true)
      val cienv = theCaseInsensitiveEnvironmentField.get(null).asInstanceOf[java.util.Map[String, String]]
      cienv.putAll(newenv);
    } catch {
      case e: NoSuchFieldException => {
        try {
          val classes = classOf[java.util.Collections].getDeclaredClasses
          val env = System.getenv()
          classes foreach (cl => {
            if ("java.util.Collections$UnmodifiableMap" == cl.getName) {
              val field = cl.getDeclaredField("m")
              field.setAccessible(true)
              val map = field.get(env).asInstanceOf[java.util.Map[String, String]]
              // map.clear() // Not sure why this was in the code. It means we need to set all required environment variables.
              map.putAll(newenv)
            }
          })
        } catch {
          case e2: Exception => e2.printStackTrace()
        }
      }
      case e1: Exception => e1.printStackTrace()
    }
  }

  def messages = LogRecord.log("log")

  def packets = LogRecord.log("graphite")

  def awaitCond(p: ⇒ Boolean, max: Duration = (2 second), interval: Duration = 500.millis, noThrow: Boolean = false): Boolean = testKitAwaitCond(p, max, interval, noThrow)

  before {
    LogRecord.clear()
    DateTimeUtil.timeProvider = () => 1000000L
    DateTimeUtils.setCurrentMillisProvider(new MillisProvider {
      override def getMillis: Long = 1000L
    })
  }

  after {
    DateTimeUtil.timeProvider = DateTimeUtil.SystemMicroTimeProvider
    DateTimeUtils.setCurrentMillisSystem()
  }

  feature("Capture Spans") {
    info("As a programmer")
    info("I want to be able to capture Spans")
    info("so that they can be logged and emitted to Graphite")
    scenario("single Span") {
      Given("A Span is started")
      TestGraphiteServer.instance should not be null

      When("Span data is added")
      moneyTracer.startSpan("some_fun_span")
      val spanId = SpanLocal.current.get
      moneyTracer.time("test")
      moneyTracer.record("bob", "craig")
      moneyTracer.record("tom", new java.lang.Long(1))
      moneyTracer.record("marty", new java.lang.String("foo"))
      moneyTracer.record("larry", new java.lang.Double(5.5))
      moneyTracer.stopSpan()

      When("the span has completed")

      Then("a log message should be written")
      awaitCond(messages.size == 1)
      messages should contain(s"Span: [ span-id=${spanId.selfId} ][ trace-id=${spanId.traceId} ][ parent-id=${spanId.parentId} ][ span-name=some_fun_span ][ app-name=unknown ][ start-time=1000000 ][ span-duration=0 ][ span-success=true ][ bob=craig ][ larry=5.5 ][ marty=foo ][ test=1000000 ][ tom=1 ]")

      And("four metrics should be sent to Graphite")
      packets.size should equal(4)

      packets should contain(s"unknown.${localHostName.replace(".", "_")}.request-tracing-data.some_fun_span.span-duration 0 1\n")
      packets should contain(s"unknown.${localHostName.replace(".", "_")}.request-tracing-data.some_fun_span.larry 5.5 1\n")
      packets should contain(s"unknown.${localHostName.replace(".", "_")}.request-tracing-data.some_fun_span.test 1000000 1\n")
      packets should contain(s"unknown.${localHostName.replace(".", "_")}.request-tracing-data.some_fun_span.tom 1 1\n")

      forAll(packets) { packet => packet shouldNot include ("marty") }
    }

    scenario("note propagation between two spans") {
      Given("a parent and a child span are started")

      TestGraphiteServer.instance should not be null

      When("Span propagatable data is added")
      moneyTracer.startSpan("parent")
      val parentSpanId = SpanLocal.current.get
      moneyTracer.time("test")
      moneyTracer.record("bob", "craig", true)
      moneyTracer.record("tom", new java.lang.Long(1))
      moneyTracer.record("marty", new java.lang.Long(4))
      moneyTracer.record("larry", new java.lang.Double(5.5))

      And("a Child span is created")
      moneyTracer.startSpan("child")
      val childSpanid = SpanLocal.current.get
      moneyTracer.record("Jerry", "Spinosa")
      moneyTracer.stopSpan()
      moneyTracer.stopSpan()

      When("both spans have completed")

      Then("a log message should be written for the parent span")
      awaitCond(messages.size == 2)
      messages should contain(s"Span: [ span-id=${parentSpanId.selfId} ][ trace-id=${parentSpanId.traceId} ][ parent-id=${parentSpanId.parentId} ][ span-name=parent ][ app-name=unknown ][ start-time=1000000 ][ span-duration=0 ][ span-success=true ][ bob=craig ][ larry=5.5 ][ marty=4 ][ test=1000000 ][ tom=1 ]")

      And("A log message should be written for hte child with the popagated note")

      messages should contain(s"Span: [ span-id=${childSpanid.selfId} ][ trace-id=${childSpanid.traceId} ][ parent-id=${childSpanid.parentId} ][ span-name=child ][ app-name=unknown ][ start-time=1000000 ][ span-duration=0 ][ span-success=true ][ Jerry=Spinosa ][ bob=craig ]")

    }

    scenario("concurrent spans") {
      Given("two spans are started at the same time")
      TestGraphiteServer.instance should not be null
      import scala.concurrent.ExecutionContext.Implicits.global
      val happySpan = future {
        val tracer = Money.tracer
        tracer.startSpan("happy")
        val spanId = SpanLocal.current.get
        tracer.time("time")
        tracer.record("apple", 5.102)
        tracer.stopSpan()
        spanId
      }
      val sadSpan = future {
        val tracer = Money.tracer
        tracer.startSpan("sad")
        val spanId = SpanLocal.current.get
        tracer.time("time")
        tracer.record("pear", 10.102)
        tracer.stopSpan()
        spanId
      }

      When("they both have completed")
      val spans = for {happyTraceId <- happySpan.mapTo[SpanId]
                       sadTraceId <- sadSpan.mapTo[SpanId]} yield (happyTraceId, sadTraceId)
      val spanIds = Await.result(spans, 3 seconds)

      Then("two log messages should be written")
      awaitCond(messages.size == 2)

      Then("a log message should be written for the first Span")
      messages should contain(s"Span: [ span-id=${spanIds._1.selfId} ][ trace-id=${spanIds._1.traceId} ][ parent-id=${spanIds._1.parentId} ][ span-name=happy ][ app-name=unknown ][ start-time=1000000 ][ span-duration=0 ][ span-success=true ][ apple=5.102 ][ time=1000000 ]")

      Then("a log message should be written for the second Span")
      messages should contain(s"Span: [ span-id=${spanIds._2.selfId} ][ trace-id=${spanIds._2.traceId} ][ parent-id=${spanIds._2.parentId} ][ span-name=sad ][ app-name=unknown ][ start-time=1000000 ][ span-duration=0 ][ span-success=true ][ pear=10.102 ][ time=1000000 ]")

      Then("six packets should have been sent to Graphite")
      packets.size should equal(6)

      Then("three metrics should be sent to Graphite for the first Span")
      packets should contain(s"unknown.${localHostName.replace(".", "_")}.request-tracing-data.happy.span-duration 0 1\n")
      packets should contain(s"unknown.${localHostName.replace(".", "_")}.request-tracing-data.happy.apple 5.102 1\n")
      packets should contain(s"unknown.${localHostName.replace(".", "_")}.request-tracing-data.happy.time 1000000 1\n")

      Then("three metrics should be sent to Graphite for the second Span")
      packets should contain(s"unknown.${localHostName.replace(".", "_")}.request-tracing-data.sad.span-duration 0 1\n")
      packets should contain(s"unknown.${localHostName.replace(".", "_")}.request-tracing-data.sad.pear 10.102 1\n")
      packets should contain(s"unknown.${localHostName.replace(".", "_")}.request-tracing-data.sad.time 1000000 1\n")
    }
  }

  feature("Send Metrics") {
    info("As a programmer")
    info("I want to be able to send individual metrics")
    info("outside the context of a Span")
    scenario("a single metric is sent") {
      Given("a single metric")
      TestGraphiteServer.instance should not be null
      Money.metrics.sendMetric("bob", 1.023)

      awaitCond(messages.size == 0 && packets.size == 1)
      Then("No messages are logged")
      And("a packet containing the metric should be sent to Graphite")
      packets should contain(s"unknown.${localHostName.replace(".", "_")}.bob 1.023 1\n")
    }
  }
  feature("Disable Money") {
    scenario("a metric is sent") {
      Given("money.enable=false")
      val conf = ConfigFactory.parseString("money.enabled=false").withFallback(defaultConfig)
      Money.MoneyFactory(conf).metrics.sendMetric("bob", 1.0)
      Then("nothing should happend")
      packets.size should equal(0)
      messages.size should equal(0)
    }
    scenario("a trace is sent") {
      Given("money.enable=false")
      val conf = ConfigFactory.parseString("money.enabled=false").withFallback(defaultConfig)
      Money.MoneyFactory(conf).tracer.startSpan("bob")
      Money.MoneyFactory(conf).tracer.stopSpan()
      Then("nothing should happend")
      packets.size should equal(0)
      messages.size should equal(0)
    }
  }

  feature("Tracer disabled") {
    scenario("a trace is sent") {
      Given("money.tracer.enabled=false")
      val conf = ConfigFactory.parseString("money.tracer.enabled=false").withFallback(defaultConfig)
      Money.MoneyFactory(conf).tracer.startSpan("bob")
      Money.MoneyFactory(conf).tracer.stopSpan()
      Then("nothing should happen")
      packets.size should equal(0)
      messages.size should equal(0)
    }
    scenario("a metric is sent") {
      Given("money.tracer.enabled=false")
      val conf = ConfigFactory.parseString("money.tracer.enabled=false").withFallback(defaultConfig)
      Money.metrics.sendMetric("bob", 1.023)
      awaitCond(messages.size == 0 && packets.size == 1)
      Then("No messages are logged")
      And("a packet containing the metric should be sent to Graphite")
      packets should contain(s"unknown.${localHostName.replace(".", "_")}.bob 1.023 1\n")
    }
  }
}
