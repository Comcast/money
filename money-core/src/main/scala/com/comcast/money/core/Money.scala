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

import com.comcast.money.api.{ InstrumentationLibrary, MoneyTracerProvider, SpanFactory, SpanHandler }
import com.comcast.money.core.async.AsyncNotifier
import com.comcast.money.core.formatters.{ Formatter, FormatterChain }
import com.comcast.money.core.handlers.HandlerChain
import com.typesafe.config.{ Config, ConfigFactory }

case class Money(
  enabled: Boolean,
  handler: SpanHandler,
  applicationName: String,
  hostName: String,
  clock: Clock,
  tracerProvider: MoneyTracerProvider,
  factory: SpanFactory,
  tracer: Tracer,
  formatter: Formatter,
  logExceptions: Boolean = false,
  formatIdsAsHex: Boolean = false,
  asyncNotifier: AsyncNotifier = new AsyncNotifier(Seq()))

object Money {

  lazy val Environment: Money = apply(ConfigFactory.load().getConfig("money"))

  def apply(conf: Config): Money = {
    val applicationName = conf.getString("application-name")
    val enabled = conf.getBoolean("enabled")
    val hostName = InetAddress.getLocalHost.getCanonicalHostName

    if (enabled) {
      val handler = HandlerChain(conf.getConfig("handling"))
      val clock = new NanoClock(SystemClock, TimeUnit.MILLISECONDS.toNanos(50L))
      val formatter = createFormatter(conf)

      val tracerProvider = new TracerProvider {
        override val clock: Clock = clock
        override val formatter: Formatter = formatter
        override val handler: SpanHandler = handler
      }

      val instrumentationLibrary = new InstrumentationLibrary("money-core", "0.10.0")
      val tracer = tracerProvider.get(instrumentationLibrary)
      val factory = tracer.spanFactory

      val logExceptions = conf.getBoolean("log-exceptions")
      val asyncNotificationHandlerChain = AsyncNotifier(conf.getConfig("async-notifier"))
      val formatIdsAsHex = conf.hasPath("format-ids-as-hex") && conf.getBoolean("format-ids-as-hex")
      Money(enabled, handler, applicationName, hostName, clock, tracerProvider, factory, tracer, formatter, logExceptions, formatIdsAsHex, asyncNotificationHandlerChain)
    } else {
      disabled(applicationName, hostName)
    }
  }

  private def createFormatter(conf: Config): Formatter = if (conf.hasPath("formatting")) {
    FormatterChain(conf.getConfig("formatting"))
  } else {
    FormatterChain.default
  }

  private def disabled(applicationName: String, hostName: String): Money =
    Money(enabled = false, DisabledSpanHandler, applicationName, hostName, SystemClock, DisabledTracerProvider, DisabledSpanFactory, DisabledTracer, DisabledFormatter)
}
