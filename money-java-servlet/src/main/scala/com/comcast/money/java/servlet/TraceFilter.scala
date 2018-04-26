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

package com.comcast.money.java.servlet

import javax.servlet._
import javax.servlet.http.{ HttpServletRequest, HttpServletRequestWrapper, HttpServletResponse }

import com.comcast.money.core.Formatters._
import com.comcast.money.core.Money
import com.comcast.money.core.internal.SpanLocal
import org.slf4j.LoggerFactory

import scala.util.{ Failure, Success }

/**
 * A Java Servlet 2.5 Filter.  Examines the inbound http request, and will set the
 * trace context for the request if the money trace header is found
 */
class TraceFilter extends Filter {

  private val MoneyTraceHeader = "X-MoneyTrace"
  private val B3TraceId = "X-B3-TraceId"
  private val B3SpanId = "X-B3-SpanId"
  private val B3ParentSpanId = "X-B3-ParentSpanId"
  private val logger = LoggerFactory.getLogger(classOf[TraceFilter])
  private val factory = Money.Environment.factory

  override def init(filterConfig: FilterConfig): Unit = {}

  override def destroy(): Unit = {}

  private val spanName = "servlet"

  override def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {

    SpanLocal.clear()
    val httpRequest = new HttpServletRequestWrapper(request.asInstanceOf[HttpServletRequest])

    val maybeMoney = Option(httpRequest.getHeader(MoneyTraceHeader))

    val moneyHeaderVal = maybeMoney map { headerValue =>
      // attempt to parse the incoming trace id (its a Try)
      fromHttpHeader(headerValue) match {
        case Success(spanId) => SpanLocal.push(factory.newSpan(spanId, spanName))
        case Failure(ex) => logger.warn("Unable to parse money trace for request header '{}'", headerValue)
      }
      headerValue
    }

    moneyHeaderVal.foreach { headerValue =>
      response match {
        case http: HttpServletResponse =>
          http.addHeader(MoneyTraceHeader, headerValue)
        case _ =>
          logger.warn("Unable to set money trace header on response, response type is not an HttpServletResponse ")
      }
    }

    val maybeB3TraceId = if (maybeMoney.isEmpty) Option(httpRequest.getHeader(B3TraceId)) else None

    maybeB3TraceId.foreach(traceIdVal => {
      val maybeParentSpanId = Option(httpRequest.getHeader(B3ParentSpanId))
      val maybeSpanId = Option(httpRequest.getHeader(B3SpanId))

      fromB3HttpHeaders(traceIdVal, maybeParentSpanId, maybeSpanId) match {
        case Success(spanId) => SpanLocal.push(factory.newSpan(spanId, spanName))
        case Failure(ex) => logger.warn(s"Unable to parse X-B3 trace for request headers: " +
          s"${B3TraceId}:'${traceIdVal}', " +
          s"${B3ParentSpanId}:'${maybeParentSpanId.getOrElse("None received")}', " +
          s"${B3SpanId}:'${maybeSpanId.getOrElse("None received")}'") +
          s"${ex.getMessage}"
      }

      response match {
        case http: HttpServletResponse =>
          http.addHeader(B3TraceId, traceIdVal)
          maybeParentSpanId.foreach(ps => http.addHeader(B3ParentSpanId, ps))
          maybeSpanId.foreach(s => http.addHeader(B3SpanId, s))
        case _ =>
          logger.warn("Unable to set X-B3 trace headers on response, response type is not an HttpServletResponse ")
      }

    })

    chain.doFilter(request, response)
  }
}
