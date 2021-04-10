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

package com.comcast.money.core

import java.io.{ PrintWriter, StringWriter }
import com.comcast.money.api.{ SpanEvent, SpanInfo }
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes

private[core] case class CoreEvent(
  name: String,
  eventAttributes: Attributes,
  timestamp: Long,
  exception: Throwable) extends SpanEvent {

  lazy val attributes: Attributes = initializeAttributes()

  private def initializeAttributes(): Attributes = {
    if (exception != null) {
      val attributeBuilder = if (eventAttributes != null) {
        eventAttributes.toBuilder
      } else {
        Attributes.builder()
      }
      attributeBuilder.put(SemanticAttributes.EXCEPTION_TYPE, exception.getClass.getCanonicalName)
      if (exception.getMessage != null) {
        attributeBuilder.put(SemanticAttributes.EXCEPTION_MESSAGE, exception.getMessage)
      }
      val writer = new StringWriter
      exception.printStackTrace(new PrintWriter(writer))
      attributeBuilder.put(SemanticAttributes.EXCEPTION_STACKTRACE, writer.toString)
      attributeBuilder.build()
    } else if (eventAttributes != null) {
      eventAttributes
    } else {
      Attributes.empty()
    }
  }
}

