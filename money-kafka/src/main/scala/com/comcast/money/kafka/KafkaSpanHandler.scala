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
import kafka.producer.{ ProducerConfig, KeyedMessage, Producer }

// We use the producer maker so that we can mock this out
trait ProducerMaker {
  def makeProducer(conf: Config): Producer[Array[Byte], Array[Byte]]
}

trait ConfigDrivenProducerMaker extends ProducerMaker {

  def makeProducer(conf: Config): Producer[Array[Byte], Array[Byte]] = {

    val props = new Properties()

    props.put("compression.codec", conf.getString("compression.codec"))
    props.put("producer.type", conf.getString("producer.type"))
    props.put("batch.num.messages", conf.getString("batch.num.messages"))
    props.put("message.send.max.retries", conf.getString("message.send.max.retries"))
    props.put("metadata.broker.list", conf.getString("metadata.broker.list"))

    new Producer[Array[Byte], Array[Byte]](new ProducerConfig(props))
  }
}

class KafkaSpanHandler extends ConfigurableHandler with ConfigDrivenProducerMaker {

  import AvroConversions._

  private[kafka] var topic: String = _
  private[kafka] var producer: Producer[Array[Byte], Array[Byte]] = _

  def configure(config: Config): Unit = {
    producer = makeProducer(config)
    topic = config.getString("topic")
  }

  def handle(span: SpanInfo): Unit = {
    producer.send(new KeyedMessage(topic, span.convertTo[Array[Byte]]))
  }
}
