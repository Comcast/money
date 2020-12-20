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

package com.comcast.money.core.handlers

import com.comcast.money.api.{ SpanHandler, SpanInfo }
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

case class HandlerChain(handlers: Seq[SpanHandler]) extends SpanHandler {

  private val logger = LoggerFactory.getLogger(classOf[HandlerChain])

  def handle(spanInfo: SpanInfo): Unit =
    handlers.foreach { handler =>
      try {
        handler.handle(spanInfo)
      } catch {
        case e: Throwable =>
          logger.error("Error handling span", e)
      }
    }
}

object HandlerChain {

  import HandlerFactory.create

  def apply(config: Config): SpanHandler = {
    val handlers = config.getConfigList("handlers")
      .asScala
      .flatMap(create)
      .toSeq

    if (config.getBoolean("async")) {
      new AsyncSpanHandler(scala.concurrent.ExecutionContext.global, HandlerChain(handlers))
    } else {
      HandlerChain(handlers)
    }
  }
}
