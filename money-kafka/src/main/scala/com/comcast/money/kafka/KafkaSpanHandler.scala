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
import java.util.Properties

import com.comcast.money.api.SpanInfo
import com.comcast.money.core.handlers.ConfigurableHandler
import com.comcast.money.wire.AvroConversions
import com.typesafe.config.Config
import org.apache.kafka.clients.producer.{ KafkaProducer, Producer, ProducerRecord }

import scala.collection.JavaConverters._

// We use the producer maker so that we can mock this out
trait ProducerMaker {
  def convertConfigToProperties(config: Config): Properties
  def createProducer(properties: Properties): Producer[Array[Byte], Array[Byte]]
}

trait ConfigDrivenProducerMaker extends ProducerMaker {

  def convertConfigToProperties(config: Config): Properties = {
    val properties = new Properties()

    for (entry <- config.entrySet.asScala) {
      val key = entry.getKey()
      val value = config.getAnyRef(key)
      properties.put(key, value)
    }

    properties
  }

  def createProducer(properties: Properties): Producer[Array[Byte], Array[Byte]] =
    new KafkaProducer[Array[Byte], Array[Byte]](properties)
}

class KafkaSpanHandler extends ConfigurableHandler with ConfigDrivenProducerMaker {

  import AvroConversions._

  private[kafka] var topic: String = _
  private[kafka] var properties: Properties = _
  private[kafka] var producer: Producer[Array[Byte], Array[Byte]] = _

  def configure(config: Config): Unit = {
    topic = config.getString("topic")
    properties = convertConfigToProperties(config)
    producer = createProducer(properties)
  }

  def handle(span: SpanInfo): Unit = {
    producer.send(new ProducerRecord[Array[Byte], Array[Byte]](topic, span.convertTo[Array[Byte]]))
  }
}
