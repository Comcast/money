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

package com.comcast.money.akka

import akka.actor.{ ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider }
import com.comcast.money.api.{ SpanFactory, SpanHandler }
import com.comcast.money.core.internal.SpanContext
import com.comcast.money.core.{ Money, Tracer }
import com.typesafe.config.{ Config, ConfigValueFactory }

/**
 * Contructs a new [[MoneyExtension]] from config and attaches it to the current [[ActorSystem]]
 *
 */

object MoneyExtension extends ExtensionId[MoneyExtension] with ExtensionIdProvider {

  /**
   * Creates instance of [[Money]] from rewritten config
   * MDC must be removed as it is not supported in an [[ActorSystem]]
   *
   * @param config Typesafe config to be rewritten and passed to Money
   * @return Money
   */

  private def toMoney(config: Config): Money =
    Money(
      conf = config.withValue(
        "mdc.enabled",
        ConfigValueFactory.fromAnyRef(false, "SLF4J's MDC is not supported in an actor system")
      )
    )

  /**
   * Configures and loads the extension when the ActorSystem starts
   * required by [[ExtensionIdProvider]] which needs the "canonical reference to the extension"
   *
   * @return MoneyExtension
   */
  override def lookup = MoneyExtension

  /**
   * Creates an instance of the [[MoneyExtension]] from config
   *
   * @param system the [[ExtendedActorSystem]] the [[MoneyExtension]] is to be attached to
   * @return MoneyExtension
   */
  override def createExtension(system: ExtendedActorSystem): MoneyExtension = {
    val config = system.settings.config.getConfig("money")
    val money: Money = toMoney(config)

    if (money.enabled)
      new MoneyExtension(
        money = money,
        traceFunction = (context: SpanContext) => new Tracer {
        override val spanFactory: SpanFactory = money.factory

        override val spanContext: SpanContext = context
      }
      )

    else
      new MoneyExtension(
        money = money,
        traceFunction = (_: SpanContext) => money.tracer
      )
  }

  /**
   * Returns the current instance of the [[MoneyExtension]]
   *
   * @param system the [[ActorSystem]] the [[MoneyExtension]] is attached to
   * @return MoneyExtension
   */
  override def get(system: ActorSystem): MoneyExtension = super.get(system)
}

/**
 * [[Extension]] to the [[ActorSystem]] to allow [[Money]] to attach to the ActorSystem.
 * The MoneyExtension is visible to any user of the ActorSystem.
 *
 * @param money         instance of Money to be attached to the [[ActorSystem]]
 * @param traceFunction the function used to construct a [[Tracer]]
 */

class MoneyExtension(money: Money, traceFunction: SpanContext => Tracer) extends Extension {
  val applicationName: String = money.applicationName
  val hostName: String = money.hostName
  val logExceptions: Boolean = money.logExceptions
  val handler: SpanHandler = money.handler
  val factory: SpanFactory = money.factory

  def tracer(implicit spanContextWithStack: SpanContextWithStack): Tracer = traceFunction(spanContextWithStack)
}

case class MoneyExtensionConfig(enabled: Boolean, applicationName: String, hostName: String, shouldLogExceptions: Boolean)
