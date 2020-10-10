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

package com.comcast.money.core.concurrent

import com.comcast.money.core.internal.{ MDCSupport, SpanLocal }
import com.comcast.money.core.logging.TraceLogging
import io.grpc.Context
import org.slf4j.MDC

import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor }

class TraceFriendlyExecutionContextExecutor(wrapped: ExecutionContext)
  extends ExecutionContextExecutor with TraceLogging {

  lazy val mdcSupport = new MDCSupport()

  override def execute(task: Runnable): Unit = {
    val context = Context.current()
    val submittingThreadsContext = MDC.getCopyOfContextMap

    wrapped.execute(
      () => {
        mdcSupport.propagateMDC(Option(submittingThreadsContext))
        try {
          context.run(task)
        } catch {
          case t: Throwable =>
            logException(t)
            throw t
        } finally {
          MDC.clear()
        }
      })
  }

  override def reportFailure(t: Throwable): Unit = wrapped.reportFailure(t)
}

object TraceFriendlyExecutionContextExecutor {
  object Implicits {
    implicit lazy val global: TraceFriendlyExecutionContextExecutor = new TraceFriendlyExecutionContextExecutor(scala.concurrent.ExecutionContext.global)
  }

  def apply(ec: ExecutionContext) = new TraceFriendlyExecutionContextExecutor(ec)
}
