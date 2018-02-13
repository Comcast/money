package com.comcast.money.http.client

import org.apache.http.HttpResponse
import org.apache.http.nio.{ContentDecoder, IOControl}
import org.apache.http.nio.protocol.HttpAsyncResponseConsumer
import org.apache.http.protocol.HttpContext

import scala.util.Try

class TracingHttpAsyncResponseConsumer[T](wrappee: HttpAsyncResponseConsumer[T], f: Try[HttpResponse] => Unit) extends HttpAsyncResponseConsumer[T] {
  override def getException: Exception = wrappee.getException

  override def failed(ex: Exception): Unit = {
    f(Try(ex))
    wrappee.failed(ex)
  }

  override def responseReceived(response: HttpResponse): Unit = {
    f(Try(response))
    wrappee.responseReceived(response)
  }

  override def isDone: Boolean = wrappee.isDone

  override def responseCompleted(context: HttpContext): Unit = wrappee.responseCompleted(context)

  override def getResult: T = wrappee.getResult

  override def consumeContent(decoder: ContentDecoder, ioctrl: IOControl): Unit = wrappee.consumeContent(decoder, ioctrl)

  override def cancel(): Boolean = wrappee.cancel()

  override def close(): Unit = wrappee.close()
}