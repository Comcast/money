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
import java.util.concurrent.{ CancellationException, Future }

import com.comcast.money.api.Span
import com.comcast.money.core.{ Formatters, Money, Tracer }
import org.apache.http.protocol.HttpContext
import org.apache.http.{ HttpHost, HttpRequest, HttpResponse }
import com.comcast.money.core.internal.SpanLocal
import com.comcast.money.core.state.State
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.concurrent.FutureCallback
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient
import org.apache.http.nio.{ ContentDecoder, ContentEncoder, IOControl }
import org.apache.http.nio.client.HttpAsyncClient
import org.apache.http.nio.protocol.{ HttpAsyncRequestProducer, HttpAsyncResponseConsumer }

import scala.util.{ Failure, Try }

object TraceFriendlyHttpAsyncSupport {
  def wrapExecute(
    httpRequest: HttpRequest,
    callback: FutureCallback[HttpResponse],
    tracer: Tracer
  )(f: FutureCallback[HttpResponse] => Future[HttpResponse]): Future[HttpResponse] = {
    // capture the current tracing state...
    val state = State.capture()

    // Put the X-MoneyTrace header in the request...
    TraceFriendlyHttpAsyncSupport.addTraceHeader(Option(httpRequest), SpanLocal.current)

    // Start timing the execution of the request...
    tracer.startTimer(HttpAsyncTraceConfig.HttpResponseTimeTraceKey)

    // Wrap the callback to intercept the response...
    val tracingCallback = new TracingFutureCallback[HttpResponse](Option(callback), state, response => {
      // Stop timing the execution of the request...
      tracer.stopTimer(HttpAsyncTraceConfig.HttpResponseTimeTraceKey)

      // Get the response code, will be 0 if response is null
      val responseCode = getResponseCode(response)
      tracer.record(HttpAsyncTraceConfig.HttpResponseCodeTraceKey, responseCode)
    })

    // Continue with the execution of the request...
    f(tracingCallback)
  }

  def wrapExecute[T](
    requestProducer: HttpAsyncRequestProducer,
    responseConsumer: HttpAsyncResponseConsumer[T],
    callback: FutureCallback[T],
    tracer: Tracer
  )(f: (HttpAsyncRequestProducer, HttpAsyncResponseConsumer[T], FutureCallback[T]) => Future[T]): Future[T] = {
    // capture the current tracing state...
    val state = State.capture()
    val span = SpanLocal.current

    // Wrap the producer interface to intercept the request...
    val tracingRequestProducer = new TracingHttpAsyncRequestProducer(requestProducer, state, (httpRequest: HttpRequest) => {
      // Put the X-MoneyTrace header in the request...
      TraceFriendlyHttpAsyncSupport.addTraceHeader(Option(httpRequest), span)

      // Start timing the execution of the request...
      tracer.startTimer(HttpAsyncTraceConfig.HttpResponseTimeTraceKey)
      httpRequest
    })

    // Wrap the consumer interface to intercept the response...
    val tracingResponseConsumer = new TracingHttpAsyncResponseConsumer[T](responseConsumer, state, (httpResponse: Try[HttpResponse]) => {
      // Stop timing the execution of the request...
      tracer.stopTimer(HttpAsyncTraceConfig.HttpResponseTimeTraceKey)

      // Get the response code, will be 0 if response is null
      val responseCode = getResponseCode(httpResponse)
      tracer.record(HttpAsyncTraceConfig.HttpResponseCodeTraceKey, responseCode)
      httpResponse
    })

    // Wrap the callback interface to restore the tracing state on the callback thread...
    val tracingCallback = new TracingFutureCallback[T](Option(callback), state, _ => {})

    // Continue with the execution of the request...
    f(tracingRequestProducer, tracingResponseConsumer, tracingCallback)
  }

  def addTraceHeader(httpRequest: Option[HttpRequest], currentSpan: Option[Span]) {
    (httpRequest, currentSpan) match {
      case (Some(request), Some(span)) => request.setHeader("X-MoneyTrace", Formatters.toHttpHeader(span.info.id))
    }
  }

  def getResponseCode(response: Try[HttpResponse]): Int =
    response map (_.getStatusLine) map (_.getStatusCode) getOrElse 0
}

/**
 * Provides a thin wrapper around [[HttpAsyncClient]] to support automatically tracing
 * requests and restoring the tracing state on the callback interfaces.
 */
class TraceFriendlyHttpAsyncClient(wrappee: HttpAsyncClient) extends HttpAsyncClient
    with java.io.Closeable {

  import com.comcast.money.http.client.TraceFriendlyHttpAsyncSupport._

  val tracer = Money.Environment.tracer

  override def execute[T](
    requestProducer: HttpAsyncRequestProducer,
    responseConsumer: HttpAsyncResponseConsumer[T],
    context: HttpContext,
    callback: FutureCallback[T]
  ): Future[T] =
    wrapExecute(requestProducer, responseConsumer, callback, tracer) {
      (tracingRequestProducer, tracingResponseConsumer, tracingCallback) => wrappee.execute(tracingRequestProducer, tracingResponseConsumer, context, tracingCallback)
    }

  override def execute[T](
    requestProducer: HttpAsyncRequestProducer,
    responseConsumer: HttpAsyncResponseConsumer[T],
    callback: FutureCallback[T]
  ): Future[T] =
    wrapExecute(requestProducer, responseConsumer, callback, tracer) {
      (tracingRequestProducer, tracingResponseConsumer, tracingCallback) => wrappee.execute(tracingRequestProducer, tracingResponseConsumer, tracingCallback)
    }

  override def execute(
    target: HttpHost,
    request: HttpRequest,
    context: HttpContext,
    callback: FutureCallback[HttpResponse]
  ): Future[HttpResponse] =
    wrapExecute(request, callback, tracer) {
      tracingCallback => wrappee.execute(target, request, context, tracingCallback)
    }

  override def execute(
    target: HttpHost,
    request: HttpRequest,
    callback: FutureCallback[HttpResponse]
  ): Future[HttpResponse] =
    wrapExecute(request, callback, tracer) {
      tracingCallback => wrappee.execute(target, request, tracingCallback)
    }

  override def execute(
    request: HttpUriRequest,
    context: HttpContext,
    callback: FutureCallback[HttpResponse]
  ): Future[HttpResponse] =
    wrapExecute(request, callback, tracer) {
      tracingCallback => wrappee.execute(request, context, tracingCallback)
    }

  override def execute(
    request: HttpUriRequest,
    callback: FutureCallback[HttpResponse]
  ): Future[HttpResponse] =
    wrapExecute(request, callback, tracer) {
      tracingCallback => wrappee.execute(request, tracingCallback)
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

/**
 * Wraps the [[FutureCallback]] callback interface and restores the captured tracing
 * state before invoking any of the wrapped methods.
 */
class TracingFutureCallback[T](wrappee: Option[FutureCallback[T]], state: State, f: Try[T] => Unit) extends FutureCallback[T] {
  override def failed(ex: Exception): Unit = state.restore {
    f(Failure(ex))
    wrappee.foreach(_.failed(ex))
  }

  override def completed(result: T): Unit = state.restore {
    f(Try(result))
    wrappee.foreach(_.completed(result))
  }

  override def cancelled(): Unit = state.restore {
    f(Failure(new CancellationException()))
    wrappee.foreach(_.cancelled())
  }
}

/**
 * Wraps the [[HttpAsyncRequestProducer]] interface to intercept the [[HttpRequest]].
 */
class TracingHttpAsyncRequestProducer(wrappee: HttpAsyncRequestProducer, state: State, f: HttpRequest => HttpRequest) extends HttpAsyncRequestProducer {
  override def generateRequest(): HttpRequest = state.restore {
    f(wrappee.generateRequest())
  }

  override def requestCompleted(context: HttpContext): Unit = state.restore {
    wrappee.requestCompleted(context)
  }

  override def failed(ex: Exception): Unit = state.restore {
    wrappee.failed(ex)
  }

  override def produceContent(encoder: ContentEncoder, ioctrl: IOControl): Unit = wrappee.produceContent(encoder, ioctrl)
  override def isRepeatable: Boolean = wrappee.isRepeatable
  override def getTarget: HttpHost = wrappee.getTarget
  override def resetRequest(): Unit = wrappee.resetRequest()
  override def close(): Unit = wrappee.close()
}

/**
 * Wraps the [[HttpAsyncResponseConsumer]] interface to intercept the completion of the
 * HTTP request, handle the [[HttpResponse]] and to restore the captured tracing state.
 */
class TracingHttpAsyncResponseConsumer[T](wrappee: HttpAsyncResponseConsumer[T], state: State, f: Try[HttpResponse] => Unit) extends HttpAsyncResponseConsumer[T] {
  override def responseReceived(response: HttpResponse): Unit = state.restore {
    f(Try(response))
    wrappee.responseReceived(response)
  }

  override def responseCompleted(context: HttpContext): Unit = state.restore {
    wrappee.responseCompleted(context)
  }

  override def failed(ex: Exception): Unit = state.restore {
    f(Failure(ex))
    wrappee.failed(ex)
  }

  override def consumeContent(decoder: ContentDecoder, ioctrl: IOControl): Unit = wrappee.consumeContent(decoder, ioctrl)
  override def isDone: Boolean = wrappee.isDone
  override def getResult: T = wrappee.getResult
  override def getException: Exception = wrappee.getException
  override def cancel(): Boolean = wrappee.cancel()
  override def close(): Unit = wrappee.close()
}