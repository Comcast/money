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

import io.opentelemetry.context.{ Context, ContextStorage, Scope }

private[internal] final class CoreContextStorage(
  spanContext: SpanContext,
  mdcSupport: MDCSupport,
  storage: ContextStorage) extends ContextStorage {

  override def attach(toAttach: Context): Scope = {
    val scope = storage.attach(toAttach)
    mdcSupport.setSpanMDC(spanContext.fromContext(current))
    () => {
      scope.close()
      mdcSupport.setSpanMDC(spanContext.fromContext(current))
    }
  }

  override def current: Context = storage.current
}
