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

package com.comcast.money.kafka

import akka.actor.{ ActorSystem, Props }
import akka.testkit.{ TestActorRef, TestKit }
import com.comcast.money.api
import com.comcast.money.core
import com.comcast.money.internal.EmitterProtocol.EmitSpan
import com.typesafe.config.Config
import kafka.producer.{ KeyedMessage, Producer }
import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpecLike }

trait MockProducerMaker extends ProducerMaker {

  val mockProducer = mock(classOf[Producer[Array[Byte], Array[Byte]]])

  def makeProducer(conf: Config): Producer[Array[Byte], Array[Byte]] = mockProducer
}

class TestKafkaEmitter(conf: Config) extends KafkaEmitter(conf) {

  var producerWasMade = false
  val mockProducer = mock(classOf[Producer[Array[Byte], Array[Byte]]])

  override def makeProducer(conf: Config): Producer[Array[Byte], Array[Byte]] = {
    producerWasMade = true
    mockProducer
  }
}

class KafkaEmitterSpec extends TestKit(ActorSystem("test", core.Money.config.getConfig("money.akka")))
    with WordSpecLike
    with Matchers
    with MockitoSugar
    with BeforeAndAfterAll {

  trait KafkaFixture {
    val testConfig = mock[Config]
    when(testConfig.getString("topic")).thenReturn("test-topic")

    val testEmitter = TestActorRef(Props(classOf[TestKafkaEmitter], testConfig))
    val underlyingActor = testEmitter.underlyingActor.asInstanceOf[TestKafkaEmitter]
    val testProducer = underlyingActor.mockProducer
    val sampleData = core.Span(
      new api.SpanId("foo", 1L), "key", "app", "host", 1L, true, 35L,
      Map("what" -> core.Note("what", 1L), "when" -> core.Note("when", 2L), "bob" -> core.Note("bob", "craig"))
    )
  }

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  "A KafkaEmitter" should {
    "make a producer in preStart" in new KafkaFixture {
      underlyingActor.producerWasMade shouldBe true
    }
    "send a message to the producer for a span" in new KafkaFixture {
      val span = EmitSpan(sampleData)
      testEmitter ! span

      val captor = ArgumentCaptor.forClass(classOf[KeyedMessage[Array[Byte], Array[Byte]]])
      verify(testProducer).send(captor.capture())
    }
  }
}
