package com.comcast.money.akka

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.Random

case class BinaryExponentialBackOff(slotTime: FiniteDuration, ceiling: Int = 10, stayAtCeiling: Boolean = false,
  slot: Int = 1, rand: Random = new Random(), waitTime: FiniteDuration = Duration.Zero, retries: Int = 0,
  resets: Int = 0, totalRetries: Long = 0) {
  def isStarted = retries > 0

  def reset(): BinaryExponentialBackOff = {
    copy(slot = 1, waitTime = Duration.Zero, resets = resets + 1, retries = 0)
  }

  def nextBackOff: BinaryExponentialBackOff = {
    /**
     * Inspired by:
     * http://en.wikipedia.org/wiki/Exponential_backoff
     */
    def backOffFactor = slot match {
      case 1 => if (rand.nextBoolean()) {
        0L
      } else {
        1L
      }
      case 2 => rand.nextInt(4).toLong
      case _ => {
        val max = math.round(math.pow(2, slot)) - 1
        val randomDouble = rand.nextDouble
        math.round(randomDouble * max)
      }
    }

    def backOffTime: FiniteDuration = slotTime * backOffFactor

    if (slot >= ceiling && !stayAtCeiling) {
      reset()
    } else {
      val newSlot = if (slot >= ceiling) {
        ceiling
      } else {
        slot + 1
      }
      copy(slot = newSlot, waitTime = backOffTime, retries = retries + 1, totalRetries = totalRetries + 1)
    }
  }
}
