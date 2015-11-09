package services

import com.comcast.money.core.Money.tracer
import com.comcast.money.concurrent.Futures._
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.forkjoin.ForkJoinPool
import scala.concurrent.{ExecutionContext, Future}

object Services {

  val logger = LoggerFactory.getLogger("Services")

  val ec1 = ExecutionContext.fromExecutor(new ForkJoinPool(64))
  val ec2 = ExecutionContext.fromExecutor(new ForkJoinPool(64))

  def withMoney():Future[String] = newTrace("ROOT_SERVICE") {
    Thread.sleep(5)
    tracer.record("root-wait", 5.0)
    newTrace("NESTED_SERVICE") {
      Thread.sleep(5)
      tracer.record("nested-wait", 5.0)
      ", and the nested service made you wait 50 ms"
    }(ec2)
  }.map {
    case nestedMsg =>
      tracer.time("root-done-time")
      " root service made you wait 100 ms, " + nestedMsg
  }(ec1)

  def withoutMoney():Future[String] = Future {
    Thread.sleep(5)
    logger.info("root-wait=5.0")
    Future {
      Thread.sleep(5)
      logger.info("nested-wait=5.0")
      ", and the nested service made you wait 50 ms"
    }(ec2)
  }.map {
    case nestedMsg =>
      logger.info("root-done-time " + System.currentTimeMillis())
      " root service made you wait 100 ms, " + nestedMsg
  }(ec1)
}
