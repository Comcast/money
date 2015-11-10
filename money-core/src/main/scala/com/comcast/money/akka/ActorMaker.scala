package com.comcast.money.akka

import akka.actor.{ActorRef, ActorRefFactory, Props}

trait ActorMaker {
  def makeActor(props: Props, name: String)(implicit actorRefFactory: ActorRefFactory): ActorRef =
    actorRefFactory.actorOf(props, name)
}
