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

package com.comcast.money.http.client

import java.io.Closeable
import java.util.concurrent.{CancellationException, Future}

import com.comcast.money.api.Span
import com.comcast.money.core.{Formatters, Money, Tracer}
import org.apache.http.protocol.HttpContext
import org.apache.http.{HttpHost, HttpRequest, HttpResponse}
import com.comcast.money.core.internal.SpanLocal
import com.comcast.money.core.state.State
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.concurrent.FutureCallback
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient
import org.apache.http.nio.client.HttpAsyncClient
import org.apache.http.nio.protocol.{HttpAsyncRequestProducer, HttpAsyncResponseConsumer}

import scala.util.Try

object TraceFriendlyHttpAsyncSupport {
  def wrapExecute(httpRequest: HttpRequest, callback: FutureCallback[HttpResponse], tracer: Tracer)(f: FutureCallback[HttpResponse] => Future[HttpResponse]): Future[HttpResponse] = {
    val state = State.capture()

    TraceFriendlyHttpAsyncSupport.addTraceHeader(Option(httpRequest), SpanLocal.current)

    val cb = new TracingFutureHttpResponseCallback(Option(callback), state, (response: Try[HttpResponse]) => {
      tracer.stopTimer(HttpAsyncTraceConfig.HttpResponseTimeTraceKey)
      val responseCode = getResponseCode(response)
      tracer.record(HttpAsyncTraceConfig.HttpResponseCodeTraceKey, responseCode)
    })

    tracer.startTimer(HttpAsyncTraceConfig.HttpResponseTimeTraceKey)

    f(cb)
  }

  def wrapExecute[T](requestProducer: HttpAsyncRequestProducer, responseConsumer: HttpAsyncResponseConsumer[T], callback: FutureCallback[T], tracer: Tracer)(f: (HttpAsyncRequestProducer, HttpAsyncResponseConsumer[T], FutureCallback[T]) => Future[T]): Future[T] = {
    val state = State.capture()
    val span = SpanLocal.current
    val p = new TracingHttpAsyncRequestProducer(requestProducer, (httpRequest: HttpRequest) => {
      TraceFriendlyHttpAsyncSupport.addTraceHeader(Option(httpRequest), span)
      tracer.startTimer(HttpAsyncTraceConfig.HttpResponseTimeTraceKey)
      httpRequest
    })
    val c = new TracingHttpAsyncResponseConsumer[T](responseConsumer, (httpResponse: Try[HttpResponse]) => {
      tracer.stopTimer(HttpAsyncTraceConfig.HttpResponseTimeTraceKey)
      val responseCode = getResponseCode(httpResponse)
      tracer.record(HttpAsyncTraceConfig.HttpResponseCodeTraceKey, responseCode)
      httpResponse
    })
    val cb = new TracingFutureCallback[T](Option(callback), state)
    f(p, c, cb)
  }

  def addTraceHeader(httpRequest: Option[HttpRequest], currentSpan: Option[Span]) {
    (httpRequest, currentSpan) match {
      case (Some(r), Some(s)) => r.setHeader("X-MoneyTrace", Formatters.toHttpHeader(s.info.id))
    }
  }

  def getResponseCode(response: Try[HttpResponse]): Int = {
    response map(_.getStatusLine) map(_.getStatusCode) getOrElse 0
  }
}

class TraceFriendlyHttpAsyncClient(wrappee : HttpAsyncClient) extends HttpAsyncClient with java.io.Closeable {
  import com.comcast.money.http.client.TraceFriendlyHttpAsyncSupport._

  private val tracer = Money.Environment.tracer

  override def execute[T](requestProducer: HttpAsyncRequestProducer, responseConsumer: HttpAsyncResponseConsumer[T], context: HttpContext, callback: FutureCallback[T]): Future[T] =
    wrapExecute(requestProducer, responseConsumer, callback, tracer) {
      (p, c, cb) => wrappee.execute(p, c, context, cb)
    }

  override def execute[T](requestProducer: HttpAsyncRequestProducer, responseConsumer: HttpAsyncResponseConsumer[T], callback: FutureCallback[T]): Future[T] =
    wrapExecute(requestProducer, responseConsumer, callback, tracer) {
      (p, c, cb) => wrappee.execute(p, c, cb)
    }

  override def execute(target: HttpHost, request: HttpRequest, context: HttpContext, callback: FutureCallback[HttpResponse]): Future[HttpResponse] =
    wrapExecute(request, callback, tracer) {
      cb => wrappee.execute(target, request, context, cb)
    }

  override def execute(target: HttpHost, request: HttpRequest, callback: FutureCallback[HttpResponse]): Future[HttpResponse] =
    wrapExecute(request, callback, tracer) {
      cb => wrappee.execute(target, request, cb)
    }

  override def execute(request: HttpUriRequest, context: HttpContext, callback: FutureCallback[HttpResponse]): Future[HttpResponse] =
    wrapExecute(request, callback, tracer) {
      cb => wrappee.execute(request, context, cb)
    }

  override def execute(request: HttpUriRequest, callback: FutureCallback[HttpResponse]): Future[HttpResponse] =
    wrapExecute(request, callback, tracer) {
      cb => wrappee.execute(request, cb)
    }

  override def close(): Unit = {
    wrappee match {
      case closeable: CloseableHttpAsyncClient =>
        closeable.close()
      case closeable: Closeable =>
        closeable.close()
      case closeable: AutoCloseable =>
        closeable.close()
      case _ =>
    }
  }
}

class TracingFutureHttpResponseCallback(wrappee: Option[FutureCallback[HttpResponse]], state: State, f: Try[HttpResponse] => Unit) extends FutureCallback[HttpResponse] {
  override def failed(ex: Exception): Unit = state.restore() {
    f(Try(ex))
    wrappee.foreach(_.failed(ex))
  }

  override def completed(result: HttpResponse): Unit = state.restore() {
    f(Try(result))
    wrappee.foreach(_.completed(result))
  }

  override def cancelled(): Unit = state.restore() {
    f(Try(new CancellationException()))
    wrappee.foreach(_.cancelled())
  }
}