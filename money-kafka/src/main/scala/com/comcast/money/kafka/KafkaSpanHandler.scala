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
import org.apache.kafka.clients.producer.{ KafkaProducer, ProducerRecord }

import scala.collection.JavaConverters._

// We use the producer maker so that we can mock this out
trait ProducerMaker {
  def makeProducer(conf: Config): KafkaProducer[Array[Byte], Array[Byte]]
}

trait ConfigDrivenProducerMaker extends ProducerMaker {

  def makeProducer(conf: Config): KafkaProducer[Array[Byte], Array[Byte]] = {

    val props = new Properties()
  
    for (entry <- conf.entrySet.asScala) {
      val key = entry.getKey()
      val value = entry.getValue()

      value.valueType().
    }

    props.put("compression.codec", conf.getString("compression.codec"))
    props.put("producer.type", conf.getString("producer.type"))
    props.put("batch.num.messages", conf.getString("batch.num.messages"))
    props.put("message.send.max.retries", conf.getString("message.send.max.retries"))
    props.put("metadata.broker.list", conf.getString("metadata.broker.list"))
    props.put("key.serializer", conf.getString("key.serializer"))
    props.put("value.serializer", conf.getString("value.serializer"))

    new KafkaProducer[Array[Byte], Array[Byte]](props)
  }
}

class KafkaSpanHandler extends ConfigurableHandler with ConfigDrivenProducerMaker {

  import AvroConversions._

  private[kafka] var topic: String = _
  private[kafka] var producer: KafkaProducer[Array[Byte], Array[Byte]] = _

  def configure(config: Config): Unit = {
    producer = makeProducer(config)
    topic = config.getString("topic")
  }

  def handle(span: SpanInfo): Unit = {
    producer.send(new ProducerRecord[Array[Byte], Array[Byte]](topic, span.convertTo[Array[Byte]]))
  }
}
