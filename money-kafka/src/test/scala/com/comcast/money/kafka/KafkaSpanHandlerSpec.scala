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

import com.comcast.money.{ api, core }
import com.typesafe.config.{ ConfigFactory, Config }
import kafka.message.{ GZIPCompressionCodec, CompressionCodec }
import kafka.producer.{ KeyedMessage, Producer }
import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpec }

import scala.collection.JavaConversions._

trait MockProducerMaker extends ProducerMaker {

  val mockProducer = mock(classOf[Producer[Array[Byte], Array[Byte]]])

  def makeProducer(conf: Config): Producer[Array[Byte], Array[Byte]] = mockProducer
}

class TestKafkaSpanHandler extends KafkaSpanHandler {

  var producerWasMade = false
  val mockProducer = mock(classOf[Producer[Array[Byte], Array[Byte]]])

  override def makeProducer(conf: Config): Producer[Array[Byte], Array[Byte]] = {
    producerWasMade = true
    mockProducer
  }
}

class KafkaSpanHandlerSpec extends WordSpec
    with Matchers
    with MockitoSugar
    with BeforeAndAfterAll {

  trait KafkaFixture {
    val testConfig = mock[Config]
    when(testConfig.getString("topic")).thenReturn("test-topic")

    val underTest = new TestKafkaSpanHandler()
    underTest.configure(testConfig)

    val testProducer = underTest.mockProducer
    val sampleData = core.CoreSpanInfo(
      id = new api.SpanId("foo", 1L),
      name = "key",
      appName = "app",
      host = "host",
      startTimeMillis = 1L,
      success = true,
      durationMicros = 35L,
      notes = Map("what" -> api.Note.of("what", 1L), "when" -> api.Note.of("when", 2L), "bob" -> api.Note.of("bob", "craig"))
    )
  }

  "A KafkaEmitter" should {
    "make a producer in configure" in new KafkaFixture {
      underTest.producerWasMade shouldBe true
    }
    "send a message to the producer for a span" in new KafkaFixture {
      underTest.handle(sampleData)

      val captor = ArgumentCaptor.forClass(classOf[KeyedMessage[Array[Byte], Array[Byte]]])
      verify(testProducer).send(captor.capture())
    }
  }

  "A ConfigDrivenProducerMaker" should {
    "set the properties from the config" in {
      val config = ConfigFactory.parseString(
        """
          | topic = "money"
          | compression.codec = "1"
          | producer.type = "async"
          | batch.num.messages = "1"
          | message.send.max.retries = "3"
          | request.required.acks = "0"
          | metadata.broker.list = "localhost:9092"
        """.stripMargin
      )
      val testHandler = new KafkaSpanHandler()
      testHandler.configure(config)

      val producerConfig = testHandler.producer.config
      producerConfig.brokerList shouldBe "localhost:9092"
      producerConfig.compressionCodec shouldBe GZIPCompressionCodec
      producerConfig.producerType shouldBe "async"
      producerConfig.batchNumMessages shouldBe 1
      producerConfig.messageSendMaxRetries shouldBe 3
      producerConfig.requestRequiredAcks shouldBe 0
    }
  }
}
