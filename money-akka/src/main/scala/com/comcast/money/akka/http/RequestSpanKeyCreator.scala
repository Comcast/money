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
 * [[SentRequestSpanKeyCreator]] carries a single function used to create a key for a Span for a [[HttpRequest]]
 * in the [[com.comcast.money.akka.http.client.TracedHttpClient]]
 *
 * Recommended usage through [[SentRequestSpanKeyCreator]] object
 *
 * Example:
 *
 * HttpRequestSpanKeyCreator((_: HttpRequest) => "TracedHttpRequest")
 *
 */

trait SentRequestSpanKeyCreator {
  def httpRequestToKey(request: HttpRequest): String
}

/**
 * Returns a [[SentRequestSpanKeyCreator]]
 *
 * Example:
 * {{{
 *   HttpRequestSpanKeyCreator((_: HttpRequest) => "TracedHttpRequest")
 * }}}
 *
 */

object SentRequestSpanKeyCreator {
  def apply(requestToName: HttpRequest => String): SentRequestSpanKeyCreator =
    new SentRequestSpanKeyCreator {
      override def httpRequestToKey(request: HttpRequest): String = requestToName(request)
    }
}

/**
 * [[ReceivedRequestSpanKeyCreator]] carries a single function used to create a key for a Span for a [[HttpRequest]]
 * in the [[com.comcast.money.akka.http.server.MoneyTrace]] [[akka.http.scaladsl.server.Directive]]
 *
 * Recommended usage through [[ReceivedRequestSpanKeyCreator]] object
 *
 * Example:
 *
 * HttpRequestSpanKeyCreator((_: HttpRequest) => "TracedHttpRequest")
 *
 */

trait ReceivedRequestSpanKeyCreator {
  def httpRequestToKey(request: HttpRequest): String
}

/**
 * Returns a [[ReceivedRequestSpanKeyCreator]]
 *
 * Example:
 * {{{
 *   ReceivedHttpRequestSpanKeyCreator((_: HttpRequest) => "TracedHttpRequest")
 * }}}
 *
 */

object ReceivedRequestSpanKeyCreator {
  def apply(requestToName: HttpRequest => String): ReceivedRequestSpanKeyCreator =
    new ReceivedRequestSpanKeyCreator {
      override def httpRequestToKey(request: HttpRequest): String = requestToName(request)
    }
}

/**
 * The standard [[HttpRequest]] [[SentRequestSpanKeyCreator]] or [[ReceivedRequestSpanKeyCreator]] that is used
 * during creation of a [[com.comcast.money.akka.http.client.TracedHttpClient]] or a directive
 * with [[com.comcast.money.akka.http.server.MoneyTrace]]
 */

object DefaultRequestSpanKeyCreators {
  object DefaultSent extends SentRequestSpanKeyCreator {
    override def httpRequestToKey(request: HttpRequest): String = s"SENT ${request.method.value} ${request.uri.path.toString}"
  }

  object DefaultReceived extends ReceivedRequestSpanKeyCreator {
    override def httpRequestToKey(request: HttpRequest): String = s"RECEIVED ${request.method.value} ${request.uri.path.toString}"
  }
}
