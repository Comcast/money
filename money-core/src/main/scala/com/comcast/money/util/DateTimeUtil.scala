package com.comcast.money.util

/**
 * Used to look up the current system time in microseconds
 */
object DateTimeUtil {

  /**
   * Produces the current time in microseconds
   */
  type MicroTimeProvider = () => Long

  /**
   * The default time provider that uses current system time
   */
  val SystemMicroTimeProvider:MicroTimeProvider = () => System.nanoTime() / 1000L

  /**
   * Allows us to override the time provider, useful for testing purposes
   *
   */
  var timeProvider: MicroTimeProvider = SystemMicroTimeProvider

  /**
   * @return The current time UTC in microseconds
   */
  def microTime:Long = timeProvider()
  
}
