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

package com.comcast.money.core

import com.comcast.money.api.{ Span, SpanFactory, SpanHandler, SpanId }

import scala.collection.JavaConversions._

class CoreSpanFactory(handler: SpanHandler) extends SpanFactory {

  def newSpan(spanName: String): Span = newSpan(new SpanId(), spanName)

  def childSpan(childName: String, span: Span): Span = childSpan(childName, span, false)

  def childSpan(childName: String, span: Span, sticky: Boolean): Span = {
    val info = span.info
    val child = newSpan(info.id.newChildId, childName)

    if (sticky) {
      info.notes.values
        .filter(_.isSticky)
        .foreach(child.record)
    }

    child
  }

  def newSpan(spanId: SpanId, spanName: String): Span =
    new CoreSpan(
      id = spanId,
      name = spanName,
      handler = handler
    )
}
