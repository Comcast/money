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

import scala.collection.mutable.ListBuffer
import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props }
import akka.testkit.{ ImplicitSender, TestKit }
import com.comcast.money.api.{ SpanHandler, SpanInfo }
import com.comcast.money.core.handlers.HandlerChain
import com.typesafe.config.ConfigFactory
import org.scalatest._

case class ChildTestMessage(val value: Int, originalSender: ActorRef)(implicit parent: StackedSpanContext) extends BaseSpanContext()(parent)

case class TestMessage(val value: Int) extends RootSpanContext

class ChildEchoMoneyActor extends Actor with ActorLogging with MoneyActor {
  override def receive = {
    case msg: ChildTestMessage => {
      implicit val parent = msg
      tracer.startSpan("MyChildSpan")
      tracer.record("ChildKey", "child")

      tracer.stopSpan(true)
      sender ! ChildTestMessage(msg.value + 1, msg.originalSender)
    }
  }
}

class RootEchoMoneyActor extends Actor with ActorLogging with MoneyActor {
  val childProps = Props[ChildEchoMoneyActor]
  override def receive = {
    case msg: ChildTestMessage => {
      implicit val messag = msg

      tracer.record("FromChild", msg.value)
      tracer.stopSpan(true)
      msg.originalSender ! TestMessage(msg.value + 1)
    }
    case msg: TestMessage => {
      implicit val message = msg

      tracer.startSpan("MyRootSpan")
      tracer.record("MyKey", msg.value)

      context.actorOf(childProps, "ChildActor") ! ChildTestMessage(msg.value, sender)
    }
  }
}

class CollectingSpanHandler extends SpanHandler {
  val buf = ListBuffer[SpanInfo]()
  override def handle(span: SpanInfo) = buf += span
}

class MoneyExtensionSpec() extends TestKit(ActorSystem(
  "money",
  ConfigFactory.parseString("""
                            | money {
                            |  handling = {
                            |    async = false
                            |    handlers = [
                            |    {
                            |      class = "com.comcast.money.core.akka.CollectingSpanHandler"
                            |      log-level = "INFO"
                            |    }]
                            |  }
                            | }""".stripMargin)
)) with ImplicitSender
    with WordSpecLike with Matchers with BeforeAndAfterEach with BeforeAndAfterAll {

  override def beforeEach() {
    StackedSpanContext.Implicits.root.clear

    MoneyExtension(system).handler.asInstanceOf[HandlerChain]
      .handlers(0).asInstanceOf[CollectingSpanHandler].buf.clear()
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "SpanContext" should {
    "support a simple tracing method" in {
      import StackedSpanContext.Implicits.root
      val input = 23
      val actual = StackedSpanContext.tracing("Sample Span", tracer => {
        tracer.record("input", 23)
        input * 2
      })

      actual should be(46)

      val spans = MoneyExtension(system).handler.asInstanceOf[HandlerChain]
        .handlers(0).asInstanceOf[CollectingSpanHandler].buf
      spans.size should be(1)

      val tracedSpan = spans(0)
      tracedSpan.name should be("Sample Span")
      tracedSpan.startTimeMillis() should not be (0)
      tracedSpan.durationMicros() should not be (0)
    }
  }

  "MoneyExtension" must {

    "allow simple Span handling" in {
      val echo = system.actorOf(Props[RootEchoMoneyActor], "mainactor")
      echo ! TestMessage(1)

      val x = expectMsg(TestMessage(3))

      val spans = MoneyExtension(system).handler.asInstanceOf[HandlerChain]
        .handlers(0).asInstanceOf[CollectingSpanHandler].buf
      spans.size should be(2)

      val actualChildSpan = spans(0)
      actualChildSpan.name should be("MyChildSpan")
      actualChildSpan.notes().size should be(1)
      actualChildSpan.notes().containsKey("ChildKey") should be(true)
      actualChildSpan.notes().get("ChildKey").value() should be("child")

      val actualRootSpan = spans(1)
      actualRootSpan.name should be("MyRootSpan")
      actualRootSpan.notes().size should be(2)
      actualRootSpan.notes().containsKey("MyKey") should be(true)
      actualRootSpan.notes().get("MyKey").value() should be(1)
      actualRootSpan.notes().containsKey("FromChild") should be(true)
      actualRootSpan.notes().get("FromChild").value() should be(2)

      actualChildSpan.success should be(actualRootSpan.success)
      actualChildSpan.startTimeMillis() should be >= (actualRootSpan.startTimeMillis())
      actualChildSpan.id.parentId should be(actualRootSpan.id.selfId)
    }
  }
}
