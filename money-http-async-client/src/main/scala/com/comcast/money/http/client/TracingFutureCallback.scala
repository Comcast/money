package com.comcast.money.http.client

import com.comcast.money.core.state.State
import org.apache.http.concurrent.FutureCallback

class TracingFutureCallback[T](wrappee: Option[FutureCallback[T]], state: State) extends FutureCallback[T] {
  override def failed(ex: Exception): Unit = state.restore() {
    wrappee.foreach(_.failed(ex))
  }

  override def completed(result: T): Unit = state.restore() {
    wrappee.foreach(_.completed(result))
  }

  override def cancelled(): Unit = state.restore() {
    wrappee.foreach(_.cancelled())
  }
}
