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

import com.comcast.money.annotations.Traced
import com.comcast.money.core.{ Formatters, Money, Tracer }
import com.comcast.money.core.internal.ThreadLocalSpanTracer
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpUriRequest
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.{ Around, Aspect, Before, Pointcut }

@Aspect
class HttpTraceAspect extends ThreadLocalSpanTracer {

  import HttpTraceConfig._

  def tracer: Tracer = Money.Environment.tracer

  @Pointcut("execution(@com.comcast.money.annotations.Traced * *(..)) && @annotation(traceAnnotation)")
  def traced(traceAnnotation: Traced) = {}

  // Capture executions on all Http Client implementations execute method that return a response
  @Pointcut("execution(public org.apache.http.HttpResponse org.apache.http.client.HttpClient+.execute(org.apache.http.client.methods.HttpUriRequest+, ..)) && args(httpRequest)")
  def httpClientExecute(httpRequest: HttpUriRequest) {}

  // Capture executions on all public methods of HttpClient that take a request and a response handler
  @Pointcut("execution(public * org.apache.http.client.HttpClient+.execute(org.apache.http.client.methods.HttpUriRequest+, org.apache.http.client.ResponseHandler+)) && args(httpRequest)")
  def httpClientExecuteToResponseHandler(httpRequest: HttpUriRequest) {}

  // Capture executions on all response handler implementations
  @Pointcut("execution(public * org.apache.http.client.ResponseHandler+.handleResponse(org.apache.http.HttpResponse)) && args(httpResponse)")
  def httpResponseHandler(httpResponse: HttpResponse) {}

  // Capture executions on any calls to consume the response from an http request
  @Pointcut("execution(* org.apache.http.util.EntityUtils.toString(..)) || execution(* org.apache.http.util.EntityUtils.toByteArray(..)) || execution(* org.apache.http.util.EntityUtils.consumeQuietly(..))")
  def consumeHttpEntity() {}

  @Around(value = "httpClientExecute(httpRequest) && cflow(traced(traceAnnotation))", argNames = "joinPoint, httpRequest, traceAnnotation")
  def adviseHttpClientExecute(joinPoint: ProceedingJoinPoint, httpRequest: HttpUriRequest, traceAnnotation: Traced): AnyRef = {
    var statusCode: Int = 0
    try {
      beginHttpExecute(traceAnnotation.value)
      addTraceHeader(httpRequest)
      val httpResponse: AnyRef = joinPoint.proceed
      statusCode = getStatusCode(httpResponse)
      httpResponse
    } finally {
      tracer.record(HttpResponseCodeTraceKey, statusCode)
      endHttpExecute(traceAnnotation.value)
    }
  }

  @Around(value = "consumeHttpEntity() && cflow(traced(traceAnnotation))", argNames = "joinPoint, traceAnnotation")
  def adviseReceiveEntityBody(joinPoint: ProceedingJoinPoint, traceAnnotation: Traced): AnyRef = {
    try {
      joinPoint.proceed
    } catch {
      case ex: Throwable => {
        tracer.record(HttpResponseCodeTraceKey, 0)
        throw ex
      }
    } finally {
      endConsumeHttpEntity(traceAnnotation.value)
    }
  }

  @Before(value = "httpClientExecuteToResponseHandler(httpRequest) && cflow(traced(traceAnnotation))", argNames = "httpRequest, traceAnnotation")
  def adviseHttpClientExecuteToResponseHandler(httpRequest: HttpUriRequest, traceAnnotation: Traced) {
    beginHttpExecute(traceAnnotation.value)
    addTraceHeader(httpRequest)
  }

  @Around(value = "httpResponseHandler(httpResponse) && cflow(traced(traceAnnotation))", argNames = "joinPoint, httpResponse, traceAnnotation")
  def adviseHttpResponseHandler(joinPoint: ProceedingJoinPoint, httpResponse: HttpResponse, traceAnnotation: Traced): AnyRef = {
    var statusCodeIsNotSet = true
    try {
      endHttpExecute(traceAnnotation.value)
      tracer.record(HttpResponseCodeTraceKey, getStatusCode(httpResponse))
      statusCodeIsNotSet = false
      joinPoint.proceed
    } catch {
      case ex: Throwable => {
        if (statusCodeIsNotSet) {
          tracer.record(HttpResponseCodeTraceKey, 0)
        }
        throw ex
      }
    }
  }

  private def beginHttpExecute(key: String) {
    tracer.startTimer(HttpResponseTimeTraceKey)
    tracer.startTimer(HttpFullResponseTimeTraceKey)
  }

  private def endHttpExecute(key: String) {
    tracer.stopTimer(HttpResponseTimeTraceKey)
  }

  private def endConsumeHttpEntity(key: String) {
    tracer.stopTimer(HttpFullResponseTimeTraceKey)
    tracer.startTimer(ProcessResponseTimeTraceKey)
  }

  private def getStatusCode(possibleHttpResponse: AnyRef): Int = {

    var statusCode = 0
    if (possibleHttpResponse != null && possibleHttpResponse.isInstanceOf[HttpResponse]) {
      val response: HttpResponse = possibleHttpResponse.asInstanceOf[HttpResponse]
      if (response.getStatusLine != null) {
        statusCode = response.getStatusLine.getStatusCode
      }
    }
    statusCode
  }

  private def addTraceHeader(httpRequest: HttpUriRequest) {

    if (httpRequest != null) {
      spanContext.current.foreach {
        span =>
          httpRequest.setHeader("X-MoneyTrace", Formatters.toHttpHeader(span.info.id))
      }
    }
  }
}
