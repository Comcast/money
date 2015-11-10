package com.comcast.money.core

import java.net.InetAddress

import akka.actor.{ActorRef, ActorSystem}
import com.comcast.money.internal._
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.{MDC, LoggerFactory}

import scala.util.{Failure, Success, Try}

object Money {

  val logger = LoggerFactory.getLogger("Money")
  val defaultConfig: Config = ConfigFactory.load()
  val config: Config = resolveConf()

  // For good measure, clear out MDC before loading anything
  MDC.clear()
  val moneyFactory = MoneyFactory(config)
  lazy val actorSystem: ActorSystem = moneyFactory.actorSystem
  val tracer: Tracer = moneyFactory.tracer
  val metrics: Metrics = moneyFactory.metrics
  val isEnabled = moneyFactory.isEnabled
  val mdcEnabled = moneyFactory.isMdcEnabled
  val applicationName = config.getString("money.application-name")
  val logExceptions = config.getBoolean("money.log-exceptions")
  lazy val hostName = InetAddress.getLocalHost.getCanonicalHostName

  def resolveConf(): Config = {
    val envVariableNameOpt = discoverEnvVariable("money.override-environment-variable")
    val envConfigOpt = resolveConfigFile(envVariableNameOpt)

    envConfigOpt match {

      case Some(envConfig) =>
        if (envConfig.isEmpty) {
          throw new IllegalStateException(s"Empty or Missing application.${envVariableNameOpt.get}.conf")
        }
        //resolve host specific config
        val hostVariableNameOpt = discoverEnvVariable("money.override-hostname-variable")
        val hostConfigOpt = resolveConfigFile(hostVariableNameOpt)
        if (hostConfigOpt.isDefined && !hostConfigOpt.get.isEmpty) {
          hostConfigOpt.get.withFallback(envConfig).withFallback(defaultConfig)
        } else {
          envConfig.withFallback(defaultConfig)
        }

      case None => defaultConfig
    }
  }

  private def resolveConfigFile(specifierOpt: Option[String]): Option[Config] = specifierOpt match {

    case Some(specifier) =>
      logger.info(s"Loading specific override file application.$specifier.conf")
      Some(ConfigFactory.parseResources(s"application.$specifier.conf"))

    case None => None
  }

  private def discoverEnvVariable(variableName: String): Option[String] = Try(
    defaultConfig.getString(variableName)) match {

    case Success(varName) => sys.env.get(varName) match {
      case Some(env) => Some(env)
      case None => sys.props.get(varName)
    }

    case Failure(e) =>
      logger.warn(s"$variableName value not set.")
      None
  }

  private[money] case class MoneyFactory(config: Config) {

    val isEnabled = config.getBoolean("money.enabled")
    val isTracingEnabled = config.getBoolean("money.tracer.enabled")
    val isMdcEnabled = config.getBoolean("money.mdc.enabled")

    val logger = LoggerFactory.getLogger("MoneyFactory")

    lazy val actorSystem = {
      // for added good measure, clear out mdc before creating the actor system, or
      // we could have dispatchers with stale MDC values that we cannot get rid of
      MDC.clear()
      ActorSystem("money", config.getConfig("money"))
    }
    logger.info(s"Starting Money System with configuration ${config.getConfig("money").root().render()}")

    val (tracer: Tracer, metrics: Metrics) = isEnabled match {

      case true =>
        logger.warn("Money is enabled")
        val emitterRef = actorSystem.actorOf(Emitter.props(), "emitter")
        if (isTracingEnabled) {
          (createTracer(actorSystem, emitterRef), createMetrics(emitterRef))
        } else {
          (createDisabledTracer(), createMetrics(emitterRef))
        }

      case false =>
        logger.warn("Money is NOT enabled")
        (createDisabledTracer(), createDisabledMetrics())
    }

    private def createTracer(actorSystem: ActorSystem, emitterRef: ActorRef): Tracer = new Tracer() {
      val spanSupervisorRef = actorSystem.actorOf(SpanSupervisor.props(emitterRef), "span-supervisor")
    }

    private def createMetrics(anEmitterRef: ActorRef): Metrics = new Metrics() {
      val emitterRef = anEmitterRef
    }

    private def createDisabledTracer(): Tracer = new Tracer() {

      val spanSupervisorRef: ActorRef = null

      override def startSpan(key: String) = {}

      override def time(key: String) = {}

      override def record(key: String, measure: Double): Unit = {}

      override def record(key: String, measure: Double, propogate: Boolean): Unit = {}

      override def record(key: String, measure: String): Unit = {}

      override def record(key: String, measure: String, propogate: Boolean): Unit = {}

      override def record(key: String, measure: Long): Unit = {}

      override def record(key: String, measure: Long, propogate: Boolean): Unit = {}

      override def record(key: String, measure: Boolean): Unit = {}

      override def record(key: String, measure: Boolean, propogate: Boolean): Unit = {}

      override def stopSpan(result: Note[Boolean] = Result.success) = {}

      override def close() = {}

      override def startTimer(key: String) = {}

      override def stopTimer(key: String) = {}
    }

    private def createDisabledMetrics(): Metrics = new Metrics() {
      val emitterRef: ActorRef = null

      override def sendMetric(path: String, value: Double) = {}
    }
  }
}
