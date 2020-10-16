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

package com.comcast.money.otel

import com.comcast.money.api.{ SpanHandler, SpanInfo }
import com.comcast.money.core.handlers.ConfigurableHandler
import com.typesafe.config.Config
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo
import io.opentelemetry.sdk.trace.SpanProcessor

object OtelSpanHandler {
  val instrumentationLibraryInfo: InstrumentationLibraryInfo = InstrumentationLibraryInfo.create("money", "0.10.0")
}

class OtelSpanHandler(val processor: SpanProcessor) extends SpanHandler with ConfigurableHandler {
  /**
   * Handles a span that has been stopped.
   *
   * @param span `SpanInfo` that contains the information for the completed span
   */
  override def handle(span: SpanInfo): Unit = {
    processor.onEnd(MoneyReadableSpanData(span))
  }

  override def configure(config: Config): Unit = {

  }
}
