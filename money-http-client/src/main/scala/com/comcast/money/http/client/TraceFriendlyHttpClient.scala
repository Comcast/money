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

import com.comcast.money.core.{ Formatters, Tracer, Money }
import com.comcast.money.core.Tracers._
import com.comcast.money.core.internal.SpanLocal
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.client.{ HttpClient, ResponseHandler }
import org.apache.http.conn.ClientConnectionManager
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.params.HttpParams
import org.apache.http.protocol.HttpContext
import org.apache.http.{ HttpHost, HttpRequest, HttpResponse }

import scala.util.Try

object TraceFriendlyHttpSupport {

  def wrapSimpleExecute(httpRequest: HttpRequest, tracer: Tracer)(f: => HttpResponse): HttpResponse = {
    var responseCode = 0L
    try {
      // Put the X-MoneyTrace header in the request...
      addTraceHeader(httpRequest)

      // Time the execution of the request...
      val response = timed(HttpTraceConfig.HttpResponseTimeTraceKey, tracer)(f)

      // Get the response code, will be 0 if response is null
      responseCode = getResponseCode(response)
      response
    } finally {
      tracer.record(HttpTraceConfig.HttpResponseCodeTraceKey, responseCode)
    }
  }

  def getResponseCode(response: HttpResponse): Long = Option(response.getStatusLine).map { statusLine =>
    statusLine.getStatusCode.toLong
  } getOrElse 0L

  def addTraceHeader(httpRequest: HttpRequest) {

    if (httpRequest != null) {
      SpanLocal.current.foreach {
        span =>
          httpRequest.setHeader("X-MoneyTrace", Formatters.toHttpHeader(span.info.id))
          Formatters.toB3Headers(span.info.id)(
            httpRequest.setHeader("X-B3-TraceId", _),
            httpRequest.setHeader("X-B3-ParentSpanId", _),
            httpRequest.setHeader("X-B3-SpanId", _)
          )
      }
    }
  }
}

/**
 * Provides a thin wrapper around HttpClient to allow support tracing
 */
class TraceFriendlyHttpClient(wrapee: HttpClient) extends HttpClient with java.io.Closeable {

  import com.comcast.money.http.client.TraceFriendlyHttpSupport._

  val tracer = Money.Environment.tracer

  override def getParams: HttpParams = wrapee.getParams

  override def getConnectionManager: ClientConnectionManager = wrapee.getConnectionManager

  override def execute(request: HttpUriRequest): HttpResponse = wrapSimpleExecute(request, tracer) {
    wrapee.execute(request)
  }

  override def execute(request: HttpUriRequest, context: HttpContext): HttpResponse = wrapSimpleExecute(request, tracer) {
    wrapee.execute(request, context)
  }

  override def execute(target: HttpHost, request: HttpRequest): HttpResponse = wrapSimpleExecute(request, tracer) {
    wrapee.execute(target, request)
  }

  override def execute(target: HttpHost, request: HttpRequest, context: HttpContext): HttpResponse = wrapSimpleExecute(
    request, tracer
  ) {
    wrapee.execute(target, request, context)
  }

  /**
   * We are making a big assertion of how the response handler code works; it is expected that they
   * call one of the execute methods above that are instrumented.  In that case, the http code will have been
   * instrumented already.  If a client does something other than calling the already instrumented execute method
   * then we are screwed
   */

  override def execute[T](request: HttpUriRequest, responseHandler: ResponseHandler[_ <: T]): T = {
    wrapee.execute(request, responseHandler)
  }

  override def execute[T](request: HttpUriRequest, responseHandler: ResponseHandler[_ <: T],
    context: HttpContext): T = {
    wrapee.execute(request, responseHandler, context)
  }

  override def execute[T](target: HttpHost, request: HttpRequest, responseHandler: ResponseHandler[_ <: T]): T = {
    wrapee.execute(target, request, responseHandler)
  }

  override def execute[T](target: HttpHost, request: HttpRequest, responseHandler: ResponseHandler[_ <: T],
    context: HttpContext): T = {
    wrapee.execute(target, request, responseHandler, context)
  }

  override def close(): Unit = {
    if (wrapee.isInstanceOf[CloseableHttpClient])
      wrapee.asInstanceOf[CloseableHttpClient].close()
    else if (wrapee.isInstanceOf[Closeable])
      wrapee.asInstanceOf[Closeable].close()
    else if (wrapee.isInstanceOf[AutoCloseable])
      wrapee.asInstanceOf[AutoCloseable].close()
  }
}

class TraceFriendlyResponseHandler[T](wrapee: ResponseHandler[_ <: T], tracer: Tracer) extends ResponseHandler[T] {

  import com.comcast.money.http.client.TraceFriendlyHttpSupport._

  override def handleResponse(response: HttpResponse): T = {

    // we always want to handle the response, so swallow any exception here
    Try {
      tracer.record(HttpTraceConfig.HttpResponseCodeTraceKey, getResponseCode(response))
    }
    wrapee.handleResponse(response)
  }
}
