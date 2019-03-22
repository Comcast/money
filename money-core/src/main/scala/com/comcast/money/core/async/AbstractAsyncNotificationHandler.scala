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

/**
 * Provides a simple implementation of `AsyncNotificationHandler` where the future type matches the
 * generic type argument
 *
 * @param ev the generic future type class tag
 * @tparam T the generic future type
 */
abstract class AbstractAsyncNotificationHandler[T <: AnyRef](implicit ev: ClassTag[T]) extends AsyncNotificationHandler {

  /**
   * Determines if this handler can support the type and instance of the given future
   * returned by the method bring traced
   *
   * @param futureClass the type of the future as declared on the method being traced
   * @param future the future instance returned by the method being traced
   * @return `true` if this handler can register completion for the future instance
   */
  override def supports(futureClass: Class[_], future: AnyRef): Boolean =
    futureClass == ev.runtimeClass && futureClass.isInstance(future)

  /**
   * Registers a callback function to be invoked when the future has completed
   *
   * @param futureClass the type of the future as declared on the method being traced
   * @param future the future instance for which the callback is to be registered
   * @param f the callback function
   * @return the future instance with the completion callback registered
   */
  override def whenComplete(futureClass: Class[_], future: AnyRef)(f: Try[_] => Unit): AnyRef =
    future match {
      case matched: T if supports(futureClass, future) => whenComplete(matched, f)
      case _ => future
    }

  /**
   * Implemented by derived type to register the callback function to be invoked when the future
   * has completed
   *
   * @param future the future instance for which the callback is to be registered
   * @param f the callback function
   * @return the future instance with the completion callback registered
   */
  def whenComplete(future: T, f: Try[_] => Unit): T
}
