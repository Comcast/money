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



