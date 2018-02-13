package com.comcast.money.http.client

import org.apache.http.{HttpHost, HttpRequest}
import org.apache.http.nio.{ContentEncoder, IOControl}
import org.apache.http.nio.protocol.HttpAsyncRequestProducer
import org.apache.http.protocol.HttpContext

class TracingHttpAsyncRequestProducer(wrappee: HttpAsyncRequestProducer, f: HttpRequest => HttpRequest) extends HttpAsyncRequestProducer {
  override def resetRequest(): Unit = wrappee.resetRequest()

  override def requestCompleted(context: HttpContext): Unit = wrappee.requestCompleted(context)

  override def failed(ex: Exception): Unit = wrappee.failed(ex)

  override def generateRequest(): HttpRequest = f(wrappee.generateRequest())

  override def isRepeatable: Boolean = wrappee.isRepeatable

  override def getTarget: HttpHost = wrappee.getTarget

  override def produceContent(encoder: ContentEncoder, ioctrl: IOControl): Unit = wrappee.produceContent(encoder, ioctrl)

  override def close(): Unit = wrappee.close()
}
