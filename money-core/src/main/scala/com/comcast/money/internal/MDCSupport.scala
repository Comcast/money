package com.comcast.money.internal

import com.comcast.money.core.{SpanId, Money}
import org.slf4j.MDC
import java.util.Map

object MDCSupport {

  val LogFormat = "[ span-id=%s ][ trace-id=%s ][ parent-id=%s ]"

  def format(spanId: SpanId) = LogFormat.format(spanId.spanId, spanId.traceId, spanId.parentSpanId)
}

/**
 * Adds the ability to store a span in MDC for a magical logging experience
 * @param enabled True if mdc is enabled, false if it is disabled
 */
class MDCSupport(enabled: Boolean = Money.mdcEnabled) {

  private val MoneyTraceKey = "moneyTrace"
  private val SpanNameKey = "spanName"

  def setSpanMDC(spanId: Option[SpanId]): Unit = if (enabled) {
    spanId match {
      case Some(id) => MDC.put(MoneyTraceKey, MDCSupport.format(id))
      case None => MDC.remove(MoneyTraceKey)
    }
  }

  def propogateMDC(submittingThreadsContext: Option[Map[_,_]]): Unit = if (enabled) {
    submittingThreadsContext match {
      case Some(context: Map[_, _]) => MDC.setContextMap(context)
      case None => MDC.clear()
    }
  }

  def setSpanNameMDC(spanName:Option[String]) =
    if (enabled) {
      spanName match {
        case Some(name) => MDC.put(SpanNameKey, name)
        case None => MDC.remove(SpanNameKey)
      }
    }

  def getSpanNameMDC:Option[String] = Option(MDC.get(SpanNameKey))
}
