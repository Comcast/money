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

package com.comcast.money.core.context
import com.comcast.money.api.SpanInfo
import com.comcast.money.core.internal.SpanContext
import io.opentelemetry.context.{ Context, ContextStorage, Scope }

trait MdcContextStorageFilter extends ContextStorageFilter {
  val spanContext: SpanContext

  override def attach(context: Context, storage: ContextStorage): Scope = {
    val scope = storage.attach(context)
    updateMdc(context)
    () => {
      scope.close()
      updateMdc(storage.current)
    }
  }

  def updateMdc(context: Context): Unit =
    updateMdc(spanContext.fromContext(context)
      .map(_.info))

  def updateMdc(currentSpanInfo: Option[SpanInfo])
}
