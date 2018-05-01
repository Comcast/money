package com.comcast.money.akka

import scala.reflect.{ClassTag, classTag}

/**
  * SpanKeyCreator uses [[ClassTag]] and the Scala reflection apis to get the names of the type to be named.
  * This is then used to create a key for the [[com.comcast.money.api.Span]]
  *
  * A Span key is used to determine the location in the code that a Span was made. It is important that it is unique
  * where possible.
  *
  * [[SpanKeyCreator]] is currently a work in progress
  */
private[akka] object SpanKeyCreator {
  def nameOfType[T: ClassTag]: String = classTag[T].runtimeClass.getSimpleName
}
