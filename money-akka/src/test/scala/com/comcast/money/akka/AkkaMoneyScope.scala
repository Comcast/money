package com.comcast.money.akka

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import com.comcast.money.core.handlers.HandlerChain
import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.{MatchResult, Matcher}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}

abstract class AkkaMoneyScope(val _system: ActorSystem) extends TestKit(_system) with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {
  def this() = this {
    val configString: String =
      """
        | money {
        |  handling = {
        |    async = false
        |    handlers = [
        |    {
        |      class = "com.comcast.money.akka.CollectingSpanHandler"
        |      log-level = "INFO"
        |    }]
        |  }
        | }""".stripMargin

    ActorSystem("MoneyAkkaScope", ConfigFactory.parseString(configString))
  }

  implicit val matierializer: ActorMaterializer = ActorMaterializer()

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  override def beforeEach(): Unit =
    MoneyExtension(system)
      .handler
      .asInstanceOf[HandlerChain]
      .handlers
      .headOption
      .foreach(_.asInstanceOf[CollectingSpanHandler].clear())


  def haveSomeSpanNames(expectedSpanNames: Seq[String]) =
    Matcher {
      (maybeSpanHandler: Option[CollectingSpanHandler]) =>
        val maybeNames = maybeSpanHandler.map(_.spanInfoStack.map(_.name()))

        def checkNames(names: Seq[String]): Boolean =
          names
            .zip(expectedSpanNames)
            .map { case (expectedName, actualName) => expectedName == actualName }
            .reduce(_ == _)

        MatchResult(
          matches = {
            maybeNames match {
              case Some(spanNames) if spanNames.isEmpty => false
              case Some(spanNames) if checkNames(spanNames) => true
              case _ => false
            }
          },
          rawFailureMessage = s"Names: $maybeNames were not Some($expectedSpanNames)",
          rawNegatedFailureMessage = s"Names: $maybeNames were Some($expectedSpanNames)"
        )
    }
}
