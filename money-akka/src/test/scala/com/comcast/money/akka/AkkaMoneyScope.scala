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

  def maybeCollectingSpanHandler = maybeHandlerChain.map(_.asInstanceOf[CollectingSpanHandler])

  def clearHandlerChain = maybeHandlerChain.foreach(_.asInstanceOf[CollectingSpanHandler].clear())

  def maybeHandlerChain =
    MoneyExtension(system)
      .handler
      .asInstanceOf[HandlerChain]
      .handlers
      .headOption

  override def afterAll = TestKit.shutdownActorSystem(system)

  override def beforeEach(): Unit = clearHandlerChain

  def checkNames(names: Seq[String], expectedNames: Seq[String]): Boolean =
    names
      .zip(expectedNames)
      .map { case (expectedName, actualName) => expectedName == actualName }
      .foldLeft(true) {
        case (_, acc) if acc == false => acc
        case (a, _) if a == false => a
        case (_, acc) => acc
      }

  def haveSomeSpanNames(expectedSpanNames: Seq[String],
                        checkNames: (Seq[String], Seq[String]) => Boolean = checkNames) =
    Matcher {
      (maybeSpanHandler: Option[CollectingSpanHandler]) =>
        val maybeNames = maybeSpanHandler.map(_.spanInfoStack.map(_.name()))

        MatchResult(
          matches = {
            maybeNames match {
              case Some(spanNames) if spanNames.isEmpty => false
              case Some(spanNames) => checkNames(spanNames, expectedSpanNames)
              case _ => false
            }
          },
          rawFailureMessage = s"Names: $maybeNames were not Some($expectedSpanNames)",
          rawNegatedFailureMessage = s"Names: $maybeNames were Some($expectedSpanNames)"
        )
    }

  def haveSomeSpanNamesInNoParticularOrder(expectedSpanNames: Seq[String]) = {
    def sortedCheckNames(names: Seq[String], expectedNames: Seq[String]) = checkNames(names.sortBy(_.hashCode), expectedNames.sortBy(_.hashCode))

    haveSomeSpanNames(expectedSpanNames, sortedCheckNames)
  }
}
