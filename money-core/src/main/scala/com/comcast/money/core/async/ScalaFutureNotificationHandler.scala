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

package com.comcast.money.core.async

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Try }

class ScalaFutureNotificationHandler extends AsyncNotificationHandler {
  override def supports(future: AnyRef): Boolean =
    future != null && future.isInstanceOf[Future[_]]

  override def whenComplete(future: AnyRef, f: Try[_] => Unit)(implicit executionContext: ExecutionContext): Future[_] = {
    future.asInstanceOf[Future[_]].transform(result => {
      f(Try(result))
      result
    }, throwable => {
      f(Failure(throwable))
      throwable
    })(executionContext)
  }
}
