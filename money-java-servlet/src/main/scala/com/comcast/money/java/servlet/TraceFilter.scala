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
  private val logger = LoggerFactory.getLogger(classOf[TraceFilter])
  private val factory = Money.Environment.factory

  override def init(filterConfig: FilterConfig): Unit = {}

  override def destroy(): Unit = {}

  override def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {

    SpanLocal.clear()
    val httpRequest = new HttpServletRequestWrapper(request.asInstanceOf[HttpServletRequest])
    val incomingTraceId = Option(httpRequest.getHeader(MoneyTraceHeader)) map { incTraceId =>
      // attempt to parse the incoming trace id (its a Try)
      fromHttpHeader(incTraceId) match {
        case Success(spanId) => SpanLocal.push(factory.newSpan(spanId, "servlet"))
        case Failure(ex) => logger.warn("Unable to parse money trace for request header '{}'", incTraceId)
      }
      incTraceId
    }

    incomingTraceId.foreach { traceId =>
      response match {
        case http: HttpServletResponse =>
          http.addHeader(MoneyTraceHeader, traceId)
        case _ =>
          logger.warn("Unable to set money trace header on response, response type is not an HttpServletResponse ")
      }
    }

    chain.doFilter(request, response)
  }
}
