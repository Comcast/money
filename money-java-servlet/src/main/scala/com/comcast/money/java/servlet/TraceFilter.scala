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

import com.comcast.money.core.Money
import io.opentelemetry.context.{ Context, Scope }
import javax.servlet._
import javax.servlet.http.{ HttpServletRequest, HttpServletRequestWrapper, HttpServletResponse }
import org.slf4j.LoggerFactory

/**
 * A Java Servlet 2.5 Filter.  Examines the inbound http request, and will set the
 * trace context for the request if the money trace header or X-B3 style headers are found
 */
class TraceFilter extends Filter {

  private val logger = LoggerFactory.getLogger(classOf[TraceFilter])
  private val tracer = Money.Environment.tracer
  private val formatter = Money.Environment.formatter

  override def init(filterConfig: FilterConfig): Unit = {}

  override def destroy(): Unit = {}

  private val spanName = "servlet"

  override def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {

    val httpRequest = new HttpServletRequestWrapper(request.asInstanceOf[HttpServletRequest])

    val scope: Scope = formatter.fromHttpHeaders(httpRequest.getHeader, logger.warn) match {
      case Some(spanId) =>
        val span = tracer.spanFactory.newSpan(spanId, spanName)
        Context.root()
          .`with`(span)
          .makeCurrent()
      case None => () => ()
    }

    try {
      val httpResponse = response.asInstanceOf[HttpServletResponse]
      formatter.setResponseHeaders(httpRequest.getHeader, httpResponse.addHeader)

      chain.doFilter(request, response)
    } finally {
      scope.close()
    }
  }

}
