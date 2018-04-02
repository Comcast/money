package com.comcast.money.akka

import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, Future}

object Blocking {

  implicit class RichFuture[T](future: Future[T]) {
    def get: T = Await.result(future, 5 seconds)
  }

}
