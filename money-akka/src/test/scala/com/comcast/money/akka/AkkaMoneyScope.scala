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
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import com.comcast.money.akka.SpanHandlerMatchers.clearHandlerChain
import com.typesafe.config.ConfigFactory
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike }

import scala.concurrent.ExecutionContextExecutor

abstract class AkkaMoneyScope(override val system: ActorSystem) extends TestKit(system) with WordSpecLike with Matchers with ScalatestRouteTest with BeforeAndAfterAll with BeforeAndAfterEach {
  def this() = this{
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

  implicit val actorSystem: ActorSystem = system

  implicit val moneyExtension: MoneyExtension = MoneyExtension(system)

  implicit val matierializer: ActorMaterializer = ActorMaterializer()

  override implicit val executor: ExecutionContextExecutor = actorSystem.dispatcher

  override def afterAll = TestKit.shutdownActorSystem(system)

  override def beforeEach(): Unit = clearHandlerChain
}
