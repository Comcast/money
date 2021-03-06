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

package com.comcast.money.core.internal

import java.util
import org.slf4j.MDC
import org.slf4j.spi.MDCAdapter

object MDCSupport extends MDCSupport {
  override protected val mdc: MDCAdapter = MDC.getMDCAdapter
}

trait MDCSupport {
  protected val mdc: MDCAdapter

  def getCopyOfMDC: Option[util.Map[String, String]] = Option(mdc.getCopyOfContextMap)
  def propagateMDC(submittingThreadsContext: Option[util.Map[String, String]]): Unit =
    submittingThreadsContext match {
      case Some(context: util.Map[String, String]) => mdc.setContextMap(context)
      case None => mdc.clear()
    }
}