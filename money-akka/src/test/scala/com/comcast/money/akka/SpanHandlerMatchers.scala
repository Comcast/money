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

import akka.actor.ActorSystem
import com.comcast.money.core.handlers.HandlerChain
import org.scalatest.matchers.{ MatchResult, Matcher }

object SpanHandlerMatchers {
  def maybeCollectingSpanHandler(implicit actorSystem: ActorSystem) = maybeHandlerChain.map(_.asInstanceOf[CollectingSpanHandler])

  def clearHandlerChain(implicit actorSystem: ActorSystem) = maybeHandlerChain.foreach(_.asInstanceOf[CollectingSpanHandler].clear())

  def maybeHandlerChain(implicit actorSystem: ActorSystem) =
    MoneyExtension(actorSystem)
      .handler
      .asInstanceOf[HandlerChain]
      .handlers
      .headOption

  def checkNames(names: Seq[String], expectedNames: Seq[String]): Boolean =
    names
      .zip(expectedNames)
      .map { case (expectedName, actualName) => expectedName == actualName }
      .foldLeft(true) {
        case (_, acc) if acc == false => acc
        case (a, _) if a == false => a
        case (_, acc) => acc
      }

  def haveSomeSpanNames(expectedSpanNames: Seq[String], checkNames: (Seq[String], Seq[String]) => Boolean = checkNames) =
    Matcher {
      (maybeSpanHandler: Option[CollectingSpanHandler]) =>
        val maybeNames = maybeSpanHandler.map(_.spanInfoStack.map(_.name()))

        MatchResult(
          matches = {
          maybeNames match {
            case Some(spanNames) if spanNames.isEmpty => false
            case Some(spanNames) if spanNames.length != expectedSpanNames.length => false
            case Some(spanNames) => checkNames(spanNames, expectedSpanNames)
            case _ => false
          }
        },
          rawFailureMessage = s"Names: $maybeNames were not Some($expectedSpanNames)",
          rawNegatedFailureMessage = s"Names: $maybeNames were Some($expectedSpanNames)"
        )
    }

  def haveSomeSpanName(expectedName: String) = haveSomeSpanNames(Seq(expectedName))

  def haveSomeSpanNamesInNoParticularOrder(expectedSpanNames: Seq[String]) = {
    def sortedCheckNames(names: Seq[String], expectedNames: Seq[String]) = checkNames(names.sortBy(_.hashCode), expectedNames.sortBy(_.hashCode))

    haveSomeSpanNames(expectedSpanNames, sortedCheckNames)
  }
}
