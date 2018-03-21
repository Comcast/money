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

package com.comcast.money.core.state

import com.comcast.money.core.internal.{ MDCSupport, SpanLocal }
import org.slf4j.MDC

object State {
  private lazy val mdcSupport = new MDCSupport()

  /**
   * Captures the state of the current tracing span so that it can be restored onto
   * a separate worker thread.
   *
   * {{{
   *   import com.comcast.money.core.state
   *
   *   def doSomethingAsynchronous(executor: ExecutorService) {
   *     val capturedState = State.capture()
   *
   *     executor.submit(new Runnable {
   *       override def run(): Unit = state.restore {
   *         // resumes on captured
   *       }
   *     })
   *   }
   * }}}
   * @return the captured tracing state
   */
  def capture(): State = {
    val span = SpanLocal.current
    val mdc = MDC.getCopyOfContextMap

    new State {
      override def restore(): RestoredState = {
        mdcSupport.propogateMDC(Option(mdc))
        SpanLocal.clear()
        span.foreach(SpanLocal.push)
        new RestoredState {
          override def close(): Unit = {
            MDC.clear()
            SpanLocal.clear()
          }
        }
      }
    }
  }
}

/** Represents the tracing state that has been captured from another thread */
trait State {
  /**
   * Restores the tracing state on the current thread returning a [[RestoredState]]
   * that can be used with Java 7+ "try-with-resources" syntax to revert the
   * restored state.
   *
   * {{{
   *   State state = State.capture();
   *   // later
   *   try (RestoredState restored = state.restore()) {
   *     // do something meaningful here
   *   }
   * }}}
   * @return the restored state
   */
  def restore(): RestoredState

  /**
   * Restores the tracing state on the current thread for the duration of the
   * function.
   *
   * {{{
   *   val state = State.capture
   *   // later
   *   state.restore {
   *     // do something meaningful here
   *   }
   * }}}
   * @param f the function to invoke
   * @tparam T the return value of the function
   * @return the value returned from the function
   */
  def restore[T](f: => T): T = {
    val restoredState = restore()
    try {
      f
    } finally {
      if (restoredState != null) restoredState.close()
    }
  }
}
