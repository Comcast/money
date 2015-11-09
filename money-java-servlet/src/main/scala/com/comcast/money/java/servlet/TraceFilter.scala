package com.comcast.money.java.servlet

import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletRequestWrapper, HttpServletResponse}

import com.comcast.money.core.SpanId
import com.comcast.money.internal.SpanLocal
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success}

/**
 * A Java Servlet 2.5 Filter.  Examines the inbound http request, and will set the
 * trace context for the request if the money trace header is found
 */
class TraceFilter extends Filter {

  private val MoneyTraceHeader = "X-MoneyTrace"
  private val logger = LoggerFactory.getLogger(classOf[TraceFilter])

  override def init(filterConfig: FilterConfig): Unit = {}

  override def destroy(): Unit = {}

  override def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {

    SpanLocal.clear()
    val httpRequest = new HttpServletRequestWrapper(request.asInstanceOf[HttpServletRequest])
    val incomingTraceId = Option(httpRequest.getHeader(MoneyTraceHeader)) map { incTrcaceId =>
      // attempt to parse the incoming trace id (its a Try)
      SpanId.fromHttpHeader(incTrcaceId) match {
        case Success(spanId) => SpanLocal.push(spanId)
        case Failure(ex) => logger.warn("Unable to parse money trace for request header '{}'", incTrcaceId)
      }
      incTrcaceId
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
