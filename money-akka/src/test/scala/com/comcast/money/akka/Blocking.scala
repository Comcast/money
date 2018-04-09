package com.comcast.money.akka

import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.concurrent.{Await, Future}

object Blocking {

  implicit class RichFuture[T](future: Future[T]) {
    def get(duration: FiniteDuration = 5 seconds): T = Await.result(future, duration)
  }

}
