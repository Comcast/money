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

package com.comcast.money.core.formatters
import com.comcast.money.api.SpanId
import com.typesafe.config.Config
import scala.collection.JavaConverters._

case class FormatterChain(formatters: Seq[Formatter]) extends Formatter {
  override def toHttpHeaders(spanId: SpanId, addHeader: (String, String) => Unit): Unit = formatters.foreach {
    formatter => formatter.toHttpHeaders(spanId, addHeader)
  }

  override def fromHttpHeaders(getHeader: String => String, log: String => Unit): Option[SpanId] = formatters.toStream.flatMap {
    formatter => formatter.fromHttpHeaders(getHeader, log)
  } headOption

  override def fields: Seq[String] = formatters.flatMap {
    formatter => formatter.fields
  }

  override def setResponseHeaders(getHeader: String => String, addHeader: (String, String) => Unit): Unit = formatters.foreach {
    formatter => formatter.setResponseHeaders(getHeader, addHeader)
  }
}

object FormatterChain {
  import FormatterFactory.create

  def apply(config: Config): FormatterChain = {
    val formatters = config.getConfigList("formatters")
      .asScala
      .map(create)
      .toSeq

    FormatterChain(formatters)
  }

  def default: FormatterChain = FormatterChain(Seq(new MoneyTraceFormatter(), new TraceContextFormatter()))
}
