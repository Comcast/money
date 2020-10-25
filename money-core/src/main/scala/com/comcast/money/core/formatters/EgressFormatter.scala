package com.comcast.money.core.formatters
import com.comcast.money.api.SpanId

/**
 * Decorates a formatter so that it writes headers for outgoing requests but does not interpret headers for incoming requests.
 */
class EgressFormatter(val formatter: Formatter) extends Formatter {
  override def toHttpHeaders(spanId: SpanId, addHeader: (String, String) => Unit): Unit =
    formatter.toHttpHeaders(spanId, addHeader)
  override def fromHttpHeaders(getHeader: String => String, log: String => Unit): Option[SpanId] = None
  override def fields: Seq[String] = Nil
}
