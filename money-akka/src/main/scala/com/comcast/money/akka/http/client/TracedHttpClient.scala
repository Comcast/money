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

package com.comcast.money.akka.http.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.http.scaladsl.settings.ConnectionPoolSettings
import com.comcast.money.akka.http.DefaultRequestSpanKeyCreators.DefaultSent
import com.comcast.money.akka.http.SentRequestSpanKeyCreator
import com.comcast.money.akka.{ MoneyExtension, SpanContextWithStack }
import com.comcast.money.api.Note
import com.comcast.money.core.Tracer

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

/**
 * TracedHttpClient will construct a new wrapper for [[akka.http.scaladsl.HttpExt.singleRequest]]
 *
 * methods represent HttpRequest Methods
 *
 * If a fresh pool is required for a long running request it can be passed per request please be aware this is NOT recommended.
 *
 * A better solution is currently a WIP
 *
 * @param actorSystem
 * @param moneyExtension
 * @param httpRequestSKC
 */

class TracedHttpClient()(implicit actorSystem: ActorSystem, moneyExtension: MoneyExtension, httpRequestSKC: SentRequestSpanKeyCreator = DefaultSent) {

  import scala.collection.immutable.Seq

  private def execute(request: HttpRequest, tracer: Tracer, maybeConnectionPoolSettings: Option[ConnectionPoolSettings]): Future[HttpResponse] = {
    implicit val executionContext: ExecutionContext = actorSystem.dispatcher

    tracer.startSpan(httpRequestSKC.httpRequestToKey(request))

    val httpExt = Http()

    val eventualResponse =
      maybeConnectionPoolSettings
        .fold {
          httpExt singleRequest request
        } {
          connectionPoolSettings => httpExt.singleRequest(request, settings = connectionPoolSettings)
        }

    eventualResponse onComplete {
      case Success(_) => tracer.stopSpan()
      case Failure(e) =>
        tracer.record(Note.of(e.getMessage, e.getStackTrace.toString))
        tracer.stopSpan(result = false)
    }

    eventualResponse
  }

  private def toRequest(httpMethod: HttpMethod, uri: String, maybeBody: Option[String], headers: Seq[HttpHeader]): HttpRequest =
    maybeBody
      .fold {
        HttpRequest(httpMethod, uri, headers = headers)
      } {
        body => HttpRequest(httpMethod, uri, entity = body, headers = headers)
      }

  def get(uri: String, maybeBody: Option[String] = None, headers: Seq[HttpHeader] = Seq.empty[HttpHeader], maybeConnectionPoolSettings: Option[ConnectionPoolSettings] = None)(implicit spanContext: SpanContextWithStack): Future[HttpResponse] =
    execute(toRequest(GET, uri, maybeBody, headers), moneyExtension.tracer, maybeConnectionPoolSettings)

  def delete(uri: String, maybeBody: Option[String] = None, headers: Seq[HttpHeader] = Seq.empty[HttpHeader], maybeConnectionPoolSettings: Option[ConnectionPoolSettings] = None)(implicit spanContext: SpanContextWithStack): Future[HttpResponse] =
    execute(toRequest(DELETE, uri, maybeBody, headers), moneyExtension.tracer, maybeConnectionPoolSettings)

  def post(uri: String, body: String, headers: Seq[HttpHeader] = Seq.empty[HttpHeader], maybeConnectionPoolSettings: Option[ConnectionPoolSettings] = None)(implicit spanContext: SpanContextWithStack): Future[HttpResponse] =
    execute(HttpRequest(POST, uri, headers, body), moneyExtension.tracer, maybeConnectionPoolSettings)

  def put(uri: String, body: String, headers: Seq[HttpHeader] = Seq.empty[HttpHeader], maybeConnectionPoolSettings: Option[ConnectionPoolSettings] = None)(implicit spanContext: SpanContextWithStack): Future[HttpResponse] =
    execute(HttpRequest(PUT, uri, headers, body), moneyExtension.tracer, maybeConnectionPoolSettings)

  def patch(uri: String, body: String, headers: Seq[HttpHeader] = Seq.empty[HttpHeader], maybeConnectionPoolSettings: Option[ConnectionPoolSettings] = None)(implicit spanContext: SpanContextWithStack): Future[HttpResponse] =
    execute(HttpRequest(PATCH, uri, headers, body), moneyExtension.tracer, maybeConnectionPoolSettings)

  def head(uri: String, headers: Seq[HttpHeader] = Seq.empty[HttpHeader], maybeConnectionPoolSettings: Option[ConnectionPoolSettings] = None)(implicit spanContext: SpanContextWithStack): Future[HttpResponse] =
    execute(HttpRequest(HEAD, uri, headers), moneyExtension.tracer, maybeConnectionPoolSettings)
}

object TracedHttpClient {
  def apply()(implicit actorSystem: ActorSystem, httpRequestSKC: SentRequestSpanKeyCreator = DefaultSent): TracedHttpClient =
    new TracedHttpClient()(actorSystem = actorSystem, moneyExtension = MoneyExtension(actorSystem), httpRequestSKC)
}
