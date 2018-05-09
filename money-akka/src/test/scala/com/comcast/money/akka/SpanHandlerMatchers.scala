package com.comcast.money.akka

import akka.actor.ActorSystem
import com.comcast.money.core.handlers.HandlerChain
import org.scalatest.matchers.{MatchResult, Matcher}


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
