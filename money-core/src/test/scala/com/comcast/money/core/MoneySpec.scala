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

import java.lang.{Class => JavaClass}
import java.util.concurrent.TimeUnit

import com.comcast.money.core.Money.MoneyFactory
import com.comcast.money.internal.SpanLocal
import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, WordSpec}
import org.slf4j.MDC

import scala.collection.JavaConversions._
import scala.util.{Failure, Success, Try}

class MoneySpec extends WordSpec with Matchers {

  "The test configuration" should {
    "use test event listener for akka" in {
      val loggers = Money.config.getStringList("money.akka.loggers")
      loggers should contain("akka.testkit.TestEventListener")
    }
    "tracer should be present" in {
      Money.tracer should not be null
    }
    "span timeout should be present" in {
      Money.config.getDuration("money.span-timeout", TimeUnit.MILLISECONDS) shouldBe 60000L
    }
    "stopped span timeout should be present" in {
      Money.config.getDuration("money.stopped-span-timeout", TimeUnit.MILLISECONDS) shouldBe 100L
    }
    "application name should be present" in {
      Money.applicationName shouldEqual "unknown"
    }
    "when disabled implementation should be empty" in {
      val moneyFactory = Money.MoneyFactory(
        ConfigFactory.parseString("money.enabled=false\nmoney.tracer.enabled=true\nmoney.mdc.enabled=true"))
      val tracer = moneyFactory.tracer
      val metrics = moneyFactory.metrics
      assert(metrics.emitterRef === null)
      assert(tracer.spanSupervisorRef === null)

      // Call the empty methods for completeness
      SpanLocal.clear()
      tracer.startSpan("foo")
      SpanLocal.current shouldEqual None

      tracer.startTimer("foo")
      tracer.stopTimer("foo")
      tracer.record(Note("foo", "bar"))
      tracer.record("foo", "bar")
      tracer.stopSpan()
      tracer.time("foo")
      tracer.close()
    }
    "should throw IllegalStateException if missing environment override file" in {
      setEnv(Map("MONEY_ENV" -> "you_ll_never_find_it"))
      Try(Money.resolveConf()) match {
        case Success(_) => fail("Expected IllegalState Exception")
        case Failure(e: Throwable) => assert(e.getClass === classOf[IllegalStateException])
      }
    }
    "should resolve environment override when it exists" in {
      setEnv(Map("MONEY_ENV" -> "found_it"))
      val conf = Money.resolveConf()
      assert(conf.getString("money.bizzare-setting") == "bozo")
      assert(conf.getBoolean("money.enabled") == false)
    }
    "should resolve system property if no environment override " in {
      sys.props += ("MONEY_ENV" -> "found_it")
      val conf = Money.resolveConf()
      assert(conf.getString("money.bizzare-setting") == "bozo")
      assert(conf.getBoolean("money.enabled") == false)
    }
    "should resolve environment property if env override and sys property exist " in {
      setEnv(Map("MONEY_ENV" -> "found_it"))
      sys.props += ("MONEY_ENV" -> "not_found_it")
      val conf = Money.resolveConf()
      assert(conf.getString("money.bizzare-setting") == "bozo")
      assert(conf.getBoolean("money.enabled") == false)
    }
    "should resolve host property if host override and sys property exist " in {
      setEnv(Map("MONEY_ENV" -> "found_it"))
      setEnv(Map("MONEY_HOSTNAME" -> "host_specific"))
      val conf = Money.resolveConf()
      assert(conf.getString("money.bizzare-setting") == "host_bozo")
      assert(conf.getBoolean("money.enabled") == false)
    }
    "should not resolve host property if host override but sys property for different host " in {
      setEnv(Map("MONEY_ENV" -> "found_it"))
      setEnv(Map("MONEY_HOSTNAME" -> "different_host_specific"))
      val conf = Money.resolveConf()
      assert(conf.getString("money.bizzare-setting") == "bozo")
      assert(conf.getBoolean("money.enabled") == false)
    }
    "have mdc enabled" in {
      Money.mdcEnabled shouldBe true
    }
    "mdc enabled should be false if configured" in {
      val moneyFactory = Money.MoneyFactory(
        ConfigFactory.parseString("money.enabled=false\nmoney.tracer.enabled=true\nmoney.mdc.enabled=false"))
      moneyFactory.isMdcEnabled shouldBe false
    }
    "logging exceptions flag should exist" in {
      Money.logExceptions shouldBe false
    }
    "clear MDC before creating the actor system" in {
      MDC.put("mdc", "stuff")
      val fact = MoneyFactory(Money.config)
      val sys = fact.actorSystem
      MDC.get("mdc") shouldBe null
    }
  }

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
              // map.clear() // Not sure why this was in the code. It means we need to set all required environment
              // variables.
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
}
