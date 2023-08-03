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

import com.comcast.money.api.{ InstrumentationLibrary, SpanFactory, SpanHandler }
import com.comcast.money.core.async.AsyncNotifier
import com.comcast.money.core.context.{ ContextStorageFilterChain, FormattedMdcContextStorageFilter }
import com.comcast.money.core.formatters.{ Formatter, FormatterChain }
import com.comcast.money.core.handlers.HandlerChain
import com.comcast.money.core.internal.SpanLocal
import com.comcast.money.core.samplers.{ AlwaysOnSampler, Sampler, SamplerFactory }
import com.typesafe.config.{ Config, ConfigFactory }
import io.opentelemetry.api.common.{ AttributeKey, Attributes }
import io.opentelemetry.context.{ Context, ContextStorage, Scope }
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes

import scala.collection.JavaConverters._
import java.net.InetAddress
import java.util.concurrent.TimeUnit

case class Money(
  enabled: Boolean,
  handler: SpanHandler,
  applicationName: String,
  hostName: String,
  factory: SpanFactory,
  tracer: Tracer,
  formatter: Formatter,
  logExceptions: Boolean = false,
  asyncNotifier: AsyncNotifier = new AsyncNotifier(Seq()))

object Money {
  lazy private val MoneyVersion: String = getClass.getPackage.getImplementationVersion

  lazy val InstrumentationLibrary = new InstrumentationLibrary("money-core", MoneyVersion)
  lazy val Environment: Money = apply(ConfigFactory.load().getConfig("money"))

  def apply(conf: Config): Money = {
    val applicationName = conf.getString("application-name")
    val enabled = conf.getBoolean("enabled")
    val hostName = InetAddress.getLocalHost.getCanonicalHostName

    if (enabled) {
      val handler = HandlerChain(conf.getConfig("handling"))
      val clock = new NanoClock(SystemClock, TimeUnit.MILLISECONDS.toNanos(50L))
      configureContextFilters(conf)
      val formatter = configureFormatter(conf)
      val sampler = configureSampler(conf)
      val resource = configureResource(applicationName, hostName, conf)
      val factory: SpanFactory = CoreSpanFactory(SpanLocal, clock, handler, formatter, sampler, Money.InstrumentationLibrary, resource)
      val tracer = new Tracer {
        override val spanFactory: SpanFactory = factory
      }
      val logExceptions = conf.getBoolean("log-exceptions")
      val asyncNotificationHandlerChain = AsyncNotifier(conf.getConfig("async-notifier"))
      Money(enabled, handler, applicationName, hostName, factory, tracer, formatter, logExceptions, asyncNotificationHandlerChain)
    } else {
      disabled(applicationName, hostName)
    }
  }

  private def configureContextFilters(conf: Config): Unit = {
    val ContextKey = "context"
    val filters = if (conf.hasPath(ContextKey)) {
      ContextStorageFilterChain(conf.getConfig(ContextKey))
    } else {
      Seq(FormattedMdcContextStorageFilter(conf))
    }
    for (filter <- filters) {
      ContextStorage.addWrapper({
        storage =>
          new ContextStorage {
            override def attach(toAttach: Context): Scope = filter.attach(toAttach, storage)
            override def current: Context = storage.current
          }
      })
    }
  }

  private def configureFormatter(conf: Config): Formatter = {
    val FormattingKey = "formatting"
    if (conf.hasPath(FormattingKey)) {
      FormatterChain(conf.getConfig(FormattingKey))
    } else {
      FormatterChain.default
    }
  }

  private def configureSampler(conf: Config): Sampler = {
    val SamplingKey = "sampling"
    if (conf.hasPath(SamplingKey)) {
      SamplerFactory.create(conf.getConfig(SamplingKey))
        .getOrElse(AlwaysOnSampler)
    } else {
      AlwaysOnSampler
    }
  }

  private def configureResource(applicationName: String, hostName: String, conf: Config): Attributes = {
    val attributesBuilder = Attributes.builder()
      .put(ResourceAttributes.SERVICE_NAME, applicationName)
      .put(ResourceAttributes.TELEMETRY_SDK_NAME, "money")
      .put(ResourceAttributes.TELEMETRY_SDK_LANGUAGE, "scala")
      .put(ResourceAttributes.TELEMETRY_SDK_VERSION, MoneyVersion)
      .put(ResourceAttributes.HOST_NAME, hostName)
    val ResourceKey = "resource"
    if (conf.hasPath(ResourceKey)) {
      val resourceConf = conf.getConfig(ResourceKey)
      for (entry <- resourceConf.entrySet().asScala) {
        val key = entry.getKey
        val value = resourceConf.getString(key)
        attributesBuilder.put(AttributeKey.stringKey(key), value)
      }
    }
    attributesBuilder.build()
  }

  private def disabled(applicationName: String, hostName: String): Money =
    Money(enabled = false, DisabledSpanHandler, applicationName, hostName, DisabledSpanFactory, DisabledTracer, DisabledFormatter)
}
