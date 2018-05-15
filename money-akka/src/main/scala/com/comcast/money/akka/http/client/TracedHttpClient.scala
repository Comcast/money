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
import akka.http.scaladsl.model.{HttpHeader, HttpRequest, HttpResponse}
import com.comcast.money.akka.http.{DefaultHttpRequestSpanKeyCreator, HttpRequestSpanKeyCreator}
import com.comcast.money.akka.{MoneyExtension, SpanContextWithStack}
import com.comcast.money.api.Note
import com.comcast.money.core.Tracer

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class TracedHttpClient(implicit actorSystem: ActorSystem, moneyExtension: MoneyExtension, httpRequestSKC: HttpRequestSpanKeyCreator = DefaultHttpRequestSpanKeyCreator) {
  import scala.collection.immutable.Seq

  private def execute(request: HttpRequest, tracer: Tracer): Future[HttpResponse] = {
    implicit val executionContext: ExecutionContext = actorSystem.dispatcher

    tracer.startSpan(httpRequestSKC.httpRequestToKey(request))

    val eventualResponse = Http() singleRequest request

    eventualResponse onComplete {
      case Success(_) => tracer.stopSpan()
      case Failure(e) =>
        tracer.record(Note.of(e.getMessage, e.getStackTrace.toString))
        tracer.stopSpan(result = false)
    }

    eventualResponse
  }

  def get(uri: String, maybeBody: Option[String] = None, headers: Seq[HttpHeader] = Seq.empty[HttpHeader])(implicit spanContext: SpanContextWithStack): Future[HttpResponse] =
    maybeBody
      .fold {
        execute(HttpRequest(uri = uri, headers = headers), moneyExtension.tracer)
      } {
        body => execute(HttpRequest(uri = uri, entity = body, headers = headers), moneyExtension.tracer)
      }
}
