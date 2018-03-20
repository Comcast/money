package com.comcast.money.akka

import akka.actor.{ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}
import com.comcast.money.api.{SpanFactory, SpanHandler}
import com.comcast.money.core.internal.SpanContext
import com.comcast.money.core.{Money, Tracer}
import com.typesafe.config.{Config, ConfigValueFactory}

object MoneyExtension extends ExtensionId[MoneyExtension] with ExtensionIdProvider {

  implicit class ConfigConverter(config: Config) {
    def asMoney: Money = {
      val rewrittenConfig = config.withValue(
        "mdc.enabled",
        ConfigValueFactory.fromAnyRef(false, "SLF4J's MDC is not supported in an actor system")
      )

      Money(rewrittenConfig)
    }
  }

  /* The lookup method is required by ExtensionIdProvider, so we return ourselves here, this
     * allows us to configure our extension to be loaded when the ActorSystem starts up */
  override def lookup = MoneyExtension


  // This method will be called by Akka to instantiate our Extension
  override def createExtension(system: ExtendedActorSystem): MoneyExtension = {
    val config = system.settings.config.getConfig("money")
    val money: Money = config.asMoney

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

  override def get(system: ActorSystem): MoneyExtension = super.get(system)
}

class MoneyExtension(money: Money,
                     traceFunction: SpanContext => Tracer) extends Extension {
  val applicationName: String = money.applicationName
  val hostName: String = money.hostName
  val logExceptions: Boolean = money.logExceptions
  val handler: SpanHandler = money.handler
  val factory: SpanFactory = money.factory

  def tracer(implicit spanContextWithStack: SpanContextWithStack): Tracer = traceFunction(spanContextWithStack)
}

case class MoneyExtensionConfig(enabled: Boolean, applicationName: String, hostName: String, shouldLogExceptions: Boolean)
