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

package com.comcast.money.akka.http

import akka.http.scaladsl.model.HttpRequest

/**
 * [[HttpRequestSpanKeyCreator]] carries a single function used to create a key for a Span for a [[HttpRequest]]
 * this is used in the [[MoneyTrace]] [[akka.http.scaladsl.server.Directive]]
 *
 * Recommended usage through [[HttpRequestSpanKeyCreator]] object
 *
 * Example:
 *
 * HttpRequestSpanKeyCreator((_: HttpRequest) => "TracedHttpRequest")
 *
 */

trait HttpRequestSpanKeyCreator {
  def httpRequestToKey(request: HttpRequest): String
}

/**
 * Returns a [[HttpRequestSpanKeyCreator]]
 *
 * Example:
 * {{{
 *   HttpRequestSpanKeyCreator((_: HttpRequest) => "TracedHttpRequest")
 * }}}
 *
 */

object HttpRequestSpanKeyCreator {
  def apply(requestToName: HttpRequest => String): HttpRequestSpanKeyCreator =
    new HttpRequestSpanKeyCreator {
      override def httpRequestToKey(request: HttpRequest): String = requestToName(request)
    }
}

/**
 * The standard [[HttpRequest]] [[HttpRequestSpanKeyCreator]] that is used if there is not a
 * [[HttpRequestSpanKeyCreator]] present when a [[MoneyTrace]] Directive is created
 *
 */

object DefaultHttpRequestSpanKeyCreator extends HttpRequestSpanKeyCreator {
  override def httpRequestToKey(request: HttpRequest): String = s"${request.method.value} ${request.uri.path.toString}"
}
