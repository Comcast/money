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
 * Decorates a formatter so that it interprets headers for incoming requests but does not write headers for outgoing requests.
 */
class IngressFormatter(val formatter: Formatter) extends Formatter {
  override def toHttpHeaders(spanId: SpanId, addHeader: (String, String) => Unit): Unit = ()
  override def fromHttpHeaders(getHeader: String => String, log: String => Unit): Option[SpanId] =
    formatter.fromHttpHeaders(getHeader, log)
  override def fields: Seq[String] = formatter.fields
}
