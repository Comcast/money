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
import scala.util.{ Success, Failure }
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._

class CoreSpanFactory(handler: SpanHandler) extends SpanFactory {

  private val logger = LoggerFactory.getLogger(classOf[CoreSpanFactory])

  def newSpan(spanName: String): Span = newSpan(new SpanId(), spanName)

  /**
   * Continues a trace by creating a child span from the given x-moneytrace header
   * value or a root span if header is malformed.
   *
   * @param childName - the name of the child span to create
   * @param getHeader - function for retrieving value of x-moneytrace header
   * @return a child span with trace id and parent id from trace context header or a new root span if the
   * traceContextHeader is malformed.
   */
  def newSpanFromHeader(childName: String, getHeader: String => String): Span =
    Formatters.fromMoneyHeader(getHeader, logger.warn) match {
      case Some(spanId) => newSpan(new SpanId(spanId.traceId, spanId.parentId), childName)
      case None => {
        logger.warn(s"creating root span because x-moneytrace header '${getHeader}' was malformed")
        newSpan(childName)
      }
    }

  def childSpan(childName: String, span: Span): Span = childSpan(childName, span, true)

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
