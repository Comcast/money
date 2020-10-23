/*
 * Copyright 2012 Comcast Cable Communications Management, LLC
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

import java.net.InetAddress
import java.util.concurrent.TimeUnit

import com.comcast.money.api.{ InstrumentationLibrary, SpanFactory, SpanHandler }
import com.comcast.money.core.async.{ AsyncNotificationHandler, AsyncNotifier }
import com.comcast.money.core.formatters.{ Formatter, FormatterChain }
import com.comcast.money.core.handlers.HandlerChain
import com.typesafe.config.{ Config, ConfigFactory }

case class Money(
  enabled: Boolean,
  handler: SpanHandler,
  applicationName: String,
  hostName: String,
  factory: SpanFactory,
  tracer: Tracer,
  formatter: Formatter,
  logExceptions: Boolean = false,
  formatIdsAsHex: Boolean = false,
  asyncNotifier: AsyncNotifier = new AsyncNotifier(Seq()))

object Money {

  val InstrumentationLibrary = new InstrumentationLibrary("money-core", "0.10.0")
  lazy val Environment: Money = apply(ConfigFactory.load().getConfig("money"))

  def apply(conf: Config): Money = {
    val applicationName = conf.getString("application-name")
    val enabled = conf.getBoolean("enabled")
    val hostName = InetAddress.getLocalHost.getCanonicalHostName

    if (enabled) {
      val handler = HandlerChain(conf.getConfig("handling"))
      val clock = new NanoClock(SystemClock, TimeUnit.MILLISECONDS.toNanos(50L))
      val formatter = if (conf.hasPath("formatting")) {
        FormatterChain(conf.getConfig("formatting"))
      } else {
        FormatterChain.default
      }
      val factory: SpanFactory = new CoreSpanFactory(clock, handler, formatter)
      val tracer = new Tracer {
        override val spanFactory: SpanFactory = factory
      }
      val logExceptions = conf.getBoolean("log-exceptions")
      val asyncNotificationHandlerChain = AsyncNotifier(conf.getConfig("async-notifier"))
      val formatIdsAsHex = conf.hasPath("format-ids-as-hex") && conf.getBoolean("format-ids-as-hex")
      Money(enabled, handler, applicationName, hostName, factory, tracer, formatter, logExceptions, formatIdsAsHex, asyncNotificationHandlerChain)
    } else {
      disabled(applicationName, hostName)
    }
  }

  private def disabled(applicationName: String, hostName: String): Money =
    Money(enabled = false, DisabledSpanHandler, applicationName, hostName, DisabledSpanFactory, DisabledTracer, DisabledFormatter)
}
