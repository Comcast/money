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

package com.comcast.money.reflect

import java.lang.annotation.Annotation
import java.lang.reflect.Method

import com.comcast.money.annotations.TracedData
import com.comcast.money.core._

trait Reflections {

  /**
   * Given a method, extracts an array of option of traced data.
   *
   * If an argument in the method invocation does not have a TracedData annotation, then the value
   * in the argument's position in the method call will be None.  Otherwise, it will have Some(TracedData)
   *
   * @param method The method invocation that we are inspecting
   * @return an Array of Option of TracedData annotations indexed by the position of the parameter in the method
   *         invocation;
   *         or an empty array if the method has no arguments
   */
  def extractTracedDataAnnotations(method: Method): Array[Option[TracedData]] =
    Option(method.getParameterAnnotations).map { anns: Array[Array[Annotation]] =>
      for (annArray <- anns) yield traceDataAnnotation(annArray)
    }.getOrElse(Array.empty)

  /**
   * Extracts notes from a given method invocation and argument array.  The resulting sequence contains the note
   * for each argument in the args array passed in.  If the argument at any position does NOT have a TracedData
   * annotation, then the value at that position in the sequence will be None.
   *
   * The result is a sequence of Option of tuple of (Note, Boolean), where the second value in the tuple indicates
   * if the note should be propagated.
   *
   * If there are no arguments on the method, an empty sequence will be returned.
   * @param method The method invocation we are inspecting
   * @param args An array of arguments passed to the method invocation
   * @return A sequence of Option of Tuple(Note, Boolean); or an empty Sequence if there are no method parameters
   */
  def extractTracedDataValues(method: Method, args: Array[AnyRef]): Seq[Option[(Note[_], Boolean)]] = {

    val paramTypes: Array[Class[_]] = method.getParameterTypes
    val tracedDataAnnotations = extractTracedDataAnnotations(method)
    for (i <- 0 until tracedDataAnnotations.length) yield {
      val arg = args(i)
      tracedDataAnnotations(i).map { ann =>
        paramTypes(i) match {
          case b if isBoolean(b) => (BooleanNote(ann.value, asOption[Boolean](arg)), ann.propagate)
          case l if isLong(l) => (LongNote(ann.value, asOption[Long](arg)), ann.propagate)
          case d if isDouble(d) => (DoubleNote(ann.value, asOption[Double](arg)), ann.propagate)
          case _ => (StringNote(ann.value, asOption[String](asString(arg))), ann.propagate)
        }
      }
    }
  }

  /**
   * Records all traced parameters for the given method invocation and argument list
   * @param method The Method invocation
   * @param args The list of arguments being passed into the method
   * @param tracer The tracer to use to record the notes
   */
  def recordTracedParameters(method: Method, args: Array[AnyRef], tracer: Tracer): Unit =
    for {
      tdOpt <- extractTracedDataValues(method, args)
      tdTuple <- tdOpt
    } {
      tracer.record(tdTuple._1, tdTuple._2)
    }

  private def isBoolean(clazz: Class[_]) = clazz == classOf[Boolean] || clazz == classOf[java.lang.Boolean]

  private def isDouble(clazz: Class[_]) = clazz == classOf[Double] || clazz == classOf[java.lang.Double]

  private def isLong(clazz: Class[_]) = clazz == classOf[Long] || clazz == classOf[java.lang.Long]

  private def traceDataAnnotation(annotations: Array[Annotation]): Option[TracedData] = {

    annotations match {
      case null => None
      case _ => annotations.collectFirst {
        case ann: TracedData => ann
      }
    }
  }

  private def asOption[T](arg: AnyRef): Option[T] = {
    if (arg == null) {
      None: Option[T]
    } else {
      Option[T](arg.asInstanceOf[T])
    }
  }

  private def asString(arg: AnyRef) = if (arg != null) arg.toString else null
}
