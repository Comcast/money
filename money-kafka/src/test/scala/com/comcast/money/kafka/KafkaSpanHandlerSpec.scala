/*
 * Copyright 2012 Comcast Cable Communications Management, LLC
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

import com.comcast.money.api.Note
import com.comcast.money.api
import com.typesafe.config.{ Config, ConfigFactory }
import kafka.message.{ CompressionCodec, GZIPCompressionCodec }
import org.apache.kafka.clients.producer.{ KafkaProducer, ProducerRecord }
import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

import scala.collection.JavaConverters._
import java.{ util => ju }

import com.comcast.money.wire.TestSpanInfo
import io.opentelemetry.api.trace.StatusCode

trait MockProducerMaker extends ProducerMaker {

  val mockProducer = mock(classOf[KafkaProducer[Array[Byte], Array[Byte]]])

  def makeProducer(conf: Config): KafkaProducer[Array[Byte], Array[Byte]] = mockProducer
}

class TestKafkaSpanHandler(config: Config) extends KafkaSpanHandler(config) {

  val mockProducer = mock(classOf[KafkaProducer[Array[Byte], Array[Byte]]])

  override def createProducer(properties: ju.Properties): KafkaProducer[Array[Byte], Array[Byte]] = {
    mockProducer
  }
}

class KafkaSpanHandlerSpec extends AnyWordSpec
  with Matchers
  with MockitoSugar
  with BeforeAndAfterAll {

  trait KafkaFixture {
    val testConfig = mock[Config]
    when(testConfig.getString("topic")).thenReturn("test-topic")

    val underTest = new TestKafkaSpanHandler(testConfig)

    val testProducer = underTest.mockProducer
    val sampleData = TestSpanInfo(
      id = api.SpanId.createNew(),
      name = "key",
      appName = "app",
      host = "host",
      startTimeNanos = 1000000L,
      status = StatusCode.OK,
      durationNanos = 35000L,
      notes = Map[String, Note[_]]("what" -> api.Note.of("what", 1L), "when" -> api.Note.of("when", 2L), "bob" -> api.Note.of("bob", "craig")).asJava)
  }

  "A KafkaEmitter" should {
    "make a producer in configure" in new KafkaFixture {
      underTest.producer shouldBe underTest.mockProducer
    }
    "send a message to the producer for a span" in new KafkaFixture {
      underTest.handle(sampleData)

      val captor = ArgumentCaptor.forClass(classOf[ProducerRecord[Array[Byte], Array[Byte]]])
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
          | bootstrap.servers = "localhost:9092"
          | key.serializer = "org.apache.kafka.common.serialization.StringSerializer"
          | value.serializer = "org.apache.kafka.common.serialization.StringSerializer"
        """.stripMargin)
      val testHandler = new KafkaSpanHandler(config)

      val producerConfig = testHandler.properties

      producerConfig.getProperty("metadata.broker.list") shouldBe "localhost:9092"
      producerConfig.getProperty("compression.codec") shouldBe "1"
      producerConfig.getProperty("producer.type") shouldBe "async"
      producerConfig.getProperty("batch.num.messages") shouldBe "1"
      producerConfig.getProperty("message.send.max.retries") shouldBe "3"
      producerConfig.getProperty("request.required.acks") shouldBe "0"
    }
  }
}
