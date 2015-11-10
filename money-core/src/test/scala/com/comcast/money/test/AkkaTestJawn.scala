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

package com.comcast.money.test

import akka.actor._
import akka.testkit.{ TestActorRef, TestKit, TestProbe }
import com.comcast.money.core.Money
import com.comcast.money.emitters.LogRecord
import com.comcast.money.akka.ActorMaker
import org.scalatest._

import scala.concurrent.duration._

class AkkaTestJawn extends TestKit(ActorSystem("money", Money.config.getConfig("money")))
    with Suite with BeforeAndAfterAll with Matchers {

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  class ProbeWrapper(probe: TestProbe) extends Actor {
    def receive = {
      case x => probe.ref forward x
    }
  }

  trait TestProbeMaker extends ActorMaker {

    var cachedProbes: scala.collection.mutable.HashMap[String, TestProbe] = _

    override def makeActor(props: Props, name: String)(implicit actorRefFactory: ActorRefFactory): ActorRef = {
      val probe = TestProbe()

      probes += (name -> probe)
      actorRefFactory.actorOf(Props(new ProbeWrapper(probe)), name)
    }

    def probes: scala.collection.mutable.HashMap[String, TestProbe] = {

      if (cachedProbes == null) {
        cachedProbes = scala.collection.mutable.HashMap[String, TestProbe]()
      }
      cachedProbes
    }
  }

  def children(testActorRef: TestActorRef[_]): scala.collection.mutable.HashMap[String, TestProbe] = {
    testActorRef.underlyingActor.asInstanceOf[TestProbeMaker].probes
  }

  def children(parent: AnyRef): scala.collection.mutable.HashMap[String, TestProbe] = {
    parent.asInstanceOf[TestProbeMaker].probes
  }

  def child(testActorRef: TestActorRef[_], named: String): TestProbe = {
    children(testActorRef)(named)
  }

  def child(parent: AnyRef, named: String): TestProbe = {
    children(parent)(named)
  }

  def expectLogMessageContaining(contains: String, wait: FiniteDuration = 2.seconds) {
    awaitCond(
      LogRecord.contains("log")(_.contains(contains)), wait, 100 milliseconds,
      s"Expected log message containing string $contains not found after $wait; messages=${
        LogRecord.log("log")
      }"
    )
  }

  def expectLogMessageContainingStrings(strings: Seq[String], wait: FiniteDuration = 2.seconds) {
    awaitCond(
      LogRecord.contains("log")(s => strings.forall(s.contains)), wait, 100 milliseconds,
      s"Expected log message containing $strings not found after $wait; messages=${LogRecord.log("log")}"
    )
  }
}
