/*
 * Copyright 2012 Comcast Cable Communications Management, LLC
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

import com.comcast.money.api.SpanId
import com.comcast.money.core.Formatters._
import com.comcast.money.core.Money
import com.comcast.money.core.internal.SpanLocal
import javax.servlet._
import javax.servlet.http.{ HttpServletRequest, HttpServletRequestWrapper, HttpServletResponse }
import org.slf4j.LoggerFactory

import scala.util.{ Failure, Success }

/**
 * A Java Servlet 2.5 Filter.  Examines the inbound http request, and will set the
 * trace context for the request if the money trace header or X-B3 style headers are found
 */
class TraceFilter extends Filter {

  private val logger = LoggerFactory.getLogger(classOf[TraceFilter])
  private val factory = Money.Environment.factory

  override def init(filterConfig: FilterConfig): Unit = {}

  override def destroy(): Unit = {}

  private val spanName = "servlet"

  override def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {

    SpanLocal.clear()
    val httpRequest = new HttpServletRequestWrapper(request.asInstanceOf[HttpServletRequest])

    val maybeSpanId: Option[SpanId] = fromHttpHeaders(httpRequest.getHeader, logger.warn)
    maybeSpanId.foreach(s => SpanLocal.push(factory.newSpan(s, spanName)))

    val httpResponse = response.asInstanceOf[HttpServletResponse]
    setResponseHeaders(httpRequest.getHeader, httpResponse.addHeader)

    chain.doFilter(request, response)
  }

}
