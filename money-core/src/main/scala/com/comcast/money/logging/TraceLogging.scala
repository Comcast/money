package com.comcast.money.logging

import com.comcast.money.core.Money
import org.slf4j.{LoggerFactory, Logger}

trait TraceLogging {

  lazy val shouldLogExceptions:Boolean = Money.logExceptions
  val logger:Logger = LoggerFactory.getLogger(classOf[TraceLogging])

  def logException(t:Throwable) = if (shouldLogExceptions) {
    logger.error("Tracing exception", t)
  }
}
