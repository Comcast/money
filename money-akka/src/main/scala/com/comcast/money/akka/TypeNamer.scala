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

package com.comcast.money.akka

import scala.reflect.{ ClassTag, classTag }

/**
 * [[TypeNamer]] uses [[ClassTag]] and the Scala reflection apis to get the names of the type to be named.
 *
 * This is then used to create a key for the [[com.comcast.money.api.Span]]
 *
 * A Span key is used to determine the location in the code that a Span was made. It is important that it is unique
 * where possible.
 *
 * [[TypeNamer]] is currently a work in progress
 */
private[akka] object TypeNamer {
  def nameOfType[T: ClassTag]: String = classTag[T].runtimeClass.getSimpleName
}
