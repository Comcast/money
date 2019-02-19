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

package com.comcast.money.core.async

import scala.reflect.ClassTag
import scala.util.Try

abstract class AbstractAsyncNotificationHandler[T <: AnyRef](implicit ev: ClassTag[T]) extends AsyncNotificationHandler {
  override def supports(futureType: Class[_], future: AnyRef): Boolean =
    futureType != null && futureType == ev.runtimeClass && futureType.isInstance(future)

  override def whenComplete(futureType: Class[_], future: AnyRef, f: Try[_] => Unit): AnyRef =
    future match {
      case matched: T => whenComplete(matched, f)
      case _ => future
    }

  def whenComplete(future: T, f: Try[_] => Unit): T
}
