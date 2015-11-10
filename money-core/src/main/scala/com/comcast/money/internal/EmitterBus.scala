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

package com.comcast.money.internal

import akka.actor.ActorRef
import akka.event.{EventBus, LookupClassification}
import com.comcast.money.internal.EmitterBus.{EmitterEvent, EmitterGroup}

object EmitterBus {

  //Types of data that can be Emitted
  trait EmitData

  //groups of subscribers to Emit to
  sealed trait EmitterGroup
  case object Trace extends EmitterGroup
  case object Metric extends EmitterGroup

  case class EmitterEvent(emitterGroup: EmitterGroup, data: EmitData)

}

class EmitterBus extends EventBus with LookupClassification {

  type Event = EmitterEvent
  type Classifier = EmitterGroup
  type Subscriber = ActorRef

  override def classify(event: Event): Classifier = event.emitterGroup

  override protected def publish(event: Event, subscriber: Subscriber): Unit = {
    subscriber ! event.data
  }

  override protected def compareSubscribers(a: Subscriber, b: Subscriber): Int = a.compareTo(b)

  override protected def mapSize(): Int = 3
}
