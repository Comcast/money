package com.comcast.money.emitters

import com.typesafe.config.Config

import scala.collection._

/**
 * Contains the messages that were received so far by the test log emitter
 */
object LogRecord {

  private val messages = new mutable.HashMap[String, mutable.Set[String]] with mutable.MultiMap[String, String]

  def clear() = messages.clear()

  def add(log: String, message: String) = messages.addBinding(log, message)

  def contains(log: String)(cond: String => Boolean) = messages.entryExists(log, cond)

  def log(name: String): Set[String] = messages.getOrElse(name, mutable.Set.empty)
}

/**
 * Overrides log emitting to append each log entry to a sequence
 * that can be interrogated for testing purposes
 */
class LogRecorder(conf: Config) extends LogEmitter(conf) {

  override def record(message: String) = {
    LogRecord.add("log", message)
  }
}
