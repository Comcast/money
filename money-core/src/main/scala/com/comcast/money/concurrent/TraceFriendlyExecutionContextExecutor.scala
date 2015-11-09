package com.comcast.money.concurrent

import com.comcast.money.internal.{MDCSupport, SpanLocal}
import com.comcast.money.logging.TraceLogging

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

import org.slf4j.MDC

class TraceFriendlyExecutionContextExecutor(wrapped:ExecutionContext) extends ExecutionContextExecutor with TraceLogging  {

  lazy val mdcSupport = new MDCSupport()

  override def execute(task: Runnable): Unit = {
    val inheritedTraceId = SpanLocal.current
    val submittingThreadsContext = MDC.getCopyOfContextMap

    wrapped.execute(new Runnable {
      override def run = {

        mdcSupport.propogateMDC(Option(submittingThreadsContext))
        SpanLocal.clear()
        inheritedTraceId.map(SpanLocal.push)
        try {
          task.run
        } catch {
          case t:Throwable =>
            logException(t)
            throw t
        } finally {
          SpanLocal.clear()
          MDC.clear()
        }
      }
    })
  }

  override def reportFailure(t: Throwable): Unit = wrapped.reportFailure(t)
}

object TraceFriendlyExecutionContextExecutor {
  object Implicits {
    implicit lazy val global:TraceFriendlyExecutionContextExecutor = new TraceFriendlyExecutionContextExecutor(scala.concurrent.ExecutionContext.global)
  }

  def apply(ec:ExecutionContext) = new TraceFriendlyExecutionContextExecutor(ec)
}
