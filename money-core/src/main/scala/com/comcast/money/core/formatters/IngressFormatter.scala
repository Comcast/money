package com.comcast.money.core.formatters
import com.comcast.money.api.SpanId

/**
 * Decorates a formatter so that it interprets headers for incoming requests but does not write headers for outgoing requests.
 */
class IngressFormatter(val formatter: Formatter) extends Formatter {
  override def toHttpHeaders(spanId: SpanId, addHeader: (String, String) => Unit): Unit = ()
  override def fromHttpHeaders(getHeader: String => String, log: String => Unit): Option[SpanId] =
    formatter.fromHttpHeaders(getHeader, log)
  override def fields: Seq[String] = formatter.fields
}
