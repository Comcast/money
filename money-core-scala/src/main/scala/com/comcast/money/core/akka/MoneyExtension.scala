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

package com.comcast.money.core.akka

import java.net.InetAddress

import akka.actor.{ ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider }
import com.comcast.money.api.{ SpanFactory, SpanHandler }
import com.comcast.money.core.handlers.HandlerChain
import com.comcast.money.core.internal.SpanThreadLocal
import com.comcast.money.core.{ CoreSpanFactory, Money, Tracer }
import com.typesafe.config.{ Config, ConfigFactory, ConfigValue, ConfigValueFactory }

object MoneyExtension extends ExtensionId[MoneyExtensionImpl] with ExtensionIdProvider {
  /* The lookup method is required by ExtensionIdProvider, so we return ourselves here, this
   * allows us to configure our extension to be loaded when the ActorSystem starts up */
  override def lookup = MoneyExtension

  // This method will be called by Akka to instantiate our Extension
  override def createExtension(system: ExtendedActorSystem): MoneyExtensionImpl =
    new MoneyExtensionImpl(system.settings.config.getConfig("money"))

  override def get(system: ActorSystem): MoneyExtensionImpl = super.get(system)
}

class MoneyExtensionImpl(originalConf: Config) extends Extension {
  // Since this Extension is a shared instance per ActorSystem we need to be threadsafe
  private val conf = originalConf.withValue(
    "mdc.enabled",
    ConfigValueFactory.fromAnyRef(false, "SLF4J's MDC is not supported in an actor system")
  )

  val applicationName = conf.getString("application-name")
  val enabled = conf.getBoolean("enabled")
  val hostName = InetAddress.getLocalHost.getCanonicalHostName

  private val (innerHandler: SpanHandler, innerFactory: SpanFactory, innerTracer: ((StackedSpanContext) => Tracer), innerLogExceptions: Boolean) = if (enabled) {
    val handler = HandlerChain(conf.getConfig("handling"))
    val factory: SpanFactory = new CoreSpanFactory(handler)
    def tracer(context: StackedSpanContext): Tracer = {
      new Tracer {
        override val spanContext = context
        override val spanFactory: SpanFactory = factory
      }
    }

    val logExceptions = conf.getBoolean("log-exceptions")

    (handler, factory, (spanContext: StackedSpanContext) => tracer(spanContext), logExceptions)
  } else {
    val disabled = Money.disabled(applicationName, hostName)
    (disabled.handler, disabled.factory, () => disabled.tracer, disabled.logExceptions)
  }

  val handler = innerHandler
  val factory = innerFactory
  def tracer(implicit spanContext: StackedSpanContext) = {
    innerTracer(spanContext)
  }
  val logExceptions = innerLogExceptions
}
