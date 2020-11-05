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

/**
 * Formats the span id into HTTP headers so that it can be propagated to other services.
 */
trait Formatter {
  def toHttpHeaders(spanId: SpanId, addHeader: (String, String) => Unit): Unit
  def fromHttpHeaders(headers: Iterable[String], getHeader: String => String, log: String => Unit = _ => {}): Option[SpanId]
  def fields: Seq[String]

  def setResponseHeaders(getHeader: String => String, addHeader: (String, String) => Unit): Unit = {
    def setResponseHeader(headerName: String): Unit = Option(getHeader(headerName)).foreach(v => addHeader(headerName, v))
    fields.foreach(setResponseHeader)
  }
}
