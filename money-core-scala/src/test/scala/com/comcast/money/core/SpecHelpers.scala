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

import com.comcast.money.api.SpanId
import com.comcast.money.core.handlers.LoggingSpanHandler
import com.typesafe.config.Config
import org.scalatest.Matchers
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{ Millis, Span }

import scala.collection.{ Set, mutable }
import scala.concurrent.duration._

object LogRecord {

  private val messages = new mutable.HashMap[String, mutable.Set[String]] with mutable.MultiMap[String, String]

  def clear() = messages.clear()

  def add(log: String, message: String) = messages.addBinding(log, message)

  def contains(log: String)(cond: String => Boolean) = messages.entryExists(log, cond)

  def log(name: String): Set[String] = messages.getOrElse(name, mutable.Set.empty)
}

class LogRecorderSpanHandler extends LoggingSpanHandler {

  override def configure(config: Config): Unit = {
    super.configure(config)
    logFunction = record
  }

  def record(message: String): Unit = LogRecord.add("log", message)
}

trait SpecHelpers extends Eventually { this: Matchers =>

  def awaitCond(condition: => Boolean, max: FiniteDuration = 2.seconds, interval: Duration = 100.millis, message: String = "failed waiting") =
    eventually {
      condition shouldBe true
    }(PatienceConfig(Span(max.toMillis, Millis), Span(interval.toMillis, Millis)))

  def expectLogMessageContaining(contains: String, wait: FiniteDuration = 2.seconds) {
    awaitCond(
      LogRecord.contains("log")(_.contains(contains)), wait, 100 milliseconds,
      s"Expected log message containing string $contains not found after $wait"
    )
  }

  def expectLogMessageContainingStrings(strings: Seq[String], wait: FiniteDuration = 2.seconds) {
    awaitCond(
      LogRecord.contains("log")(s => strings.forall(s.contains)), wait, 100 milliseconds,
      s"Expected log message containing $strings not found after $wait"
    )
  }

  def testSpan(id: SpanId) = Money.Environment.factory.newSpan(id, "test")
}
