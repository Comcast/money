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

import java.util.Properties

import kafka.producer.{KeyedMessage, Producer, ProducerConfig}

/**
 * Demonstrates how to implement a simple Kafka producer application to send data to Kafka.
 *
 * Don't read too much into the actual implementation of this class.  Its sole purpose is to showcase the use of the
 * Kafka API.
 *
 * @param topic The Kafka topic to send data to.
 * @param brokerList  Value for Kafka's `metadata.broker.list` setting.
 * @param producerConfig Additional producer configuration settings.
 */
case class KafkaProducerApp(
                             val topic: String,
                             val brokerList: String,
                             producerConfig: Properties = new Properties
                             ) {

  private val producer = {
    val effectiveConfig = {
      val c = new Properties
      c.load(this.getClass.getResourceAsStream("/producer-defaults.properties"))
      c.putAll(producerConfig)
      c.put("metadata.broker.list", brokerList)
      c
    }
    new Producer[Array[Byte], Array[Byte]](new ProducerConfig(effectiveConfig))
  }

  // The configuration field of the wrapped producer is immutable (including its nested fields), so it's safe to expose
  // it directly.
  val config = producer.config

  private def toMessage(key: Option[Array[Byte]], value: Array[Byte]): KeyedMessage[Array[Byte], Array[Byte]] =
    key match {
      case Some(key) => new KeyedMessage(topic, key, value)
      case _ => new KeyedMessage(topic, value)
    }

  def send(key: Array[Byte], value: Array[Byte]): Unit = producer.send(toMessage(Some(key), value))

  def send(value: Array[Byte]): Unit = producer.send(toMessage(None, value))

  def shutdown(): Unit = producer.close()

}

/**
 * Creates KafkaProducerApp instances.
 *
 * We require such a factory because of how Storm and notably
 * [[http://storm.incubator.apache.org/documentation/Serialization.html serialization within Storm]] work.
 * Without such a factory we cannot properly unit tests Storm bolts that need to write to Kafka.
 *
 * Preferably we would simply pass a Kafka producer directly to a Storm bolt.  During testing we could then mock this
 * collaborator.  However this intuitive approach fails at (Storm) runtime because Kafka producers are not serializable.
 * The alternative approach of instantiating the Kafka producer from within the bolt (e.g. using a `@transient lazy val`
 * field) does work at runtime but prevents us from verifying the correct interaction between our bolt's code and its
 * collaborator, the Kafka producer, because we cannot easily mock the producer in this setup.  The chosen approach of
 * the factory method, while introducing some level of unwanted indirection and complexity, is a pragmatic approach to
 * make our Storm code work correctly at runtime and to make it testable.
 *
 * @param topic The Kafka topic to send data to.
 * @param brokerList  Value for Kafka's `metadata.broker.list` setting.
 * @param config Additional producer configuration settings.
 */
abstract class KafkaProducerAppFactory(topic: String, brokerList: String, config: Properties) extends Serializable {
  def newInstance(): KafkaProducerApp
}

class BaseKafkaProducerAppFactory(topic: String, brokerList: String, config: Properties = new Properties)
  extends KafkaProducerAppFactory(topic, brokerList, config) {

  override def newInstance() = new KafkaProducerApp(topic, brokerList, config)

}
