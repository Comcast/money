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

  def capture(): State = {
    val span = SpanLocal.current
    val mdc = MDC.getCopyOfContextMap

    new State {
      override def restore()(f: => Unit): Unit = {
        mdcSupport.propogateMDC(Option(mdc))
        SpanLocal.clear()
        span.foreach(SpanLocal.push)
        try {
          f
        } finally {
          MDC.clear()
          SpanLocal.clear()
        }
      }
    }
  }
}

trait State {
  def restore()(f: => Unit): Unit
}
