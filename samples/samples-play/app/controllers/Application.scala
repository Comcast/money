package controllers

import com.comcast.money.concurrent.Futures
import com.comcast.money.core.Money.tracer
import org.slf4j.LoggerFactory
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import services.Services

import scala.concurrent.Future

object Application extends Controller {

  val logger = LoggerFactory.getLogger("SAMPLE_CONTROLLER")

  def withMoney = Action.async {
    Futures.newTrace("SAMPLE_CONTROLLER") {
      Services.withMoney()
  }.map {
      case rootMsg =>
        tracer.time("main-done-time")
        Ok("Hello, " + rootMsg)
    }
  }

  def withoutMoney = Action.async {
    Future {
      Services.withoutMoney()
    }.map {
      case rootMsg =>
        logger.info("main-done-time")
        Ok("Hello, " + rootMsg)
    }
  }
}