package com.comcast.money.core.formatters

import com.comcast.money.api.SpanId
import com.typesafe.config.Config

class ConfiguredFormatter extends ConfigurableFormatter {

  var calledConfigure = false

  def configure(config: Config): Unit = calledConfigure = true
  override def toHttpHeaders(spanId: SpanId, addHeader: (String, String) => Unit): Unit = ()
  override def fromHttpHeaders(getHeader: String => String, log: String => Unit): Option[SpanId] = None
  override def fields: Seq[String] = Nil
}

class NonConfiguredFormatter extends Formatter {
  override def toHttpHeaders(spanId: SpanId, addHeader: (String, String) => Unit): Unit = ()
  override def fromHttpHeaders(getHeader: String => String, log: String => Unit): Option[SpanId] = None
  override def fields: Seq[String] = Nil
}