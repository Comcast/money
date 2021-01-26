/*
 * Copyright 2012 Comcast Cable Communications Management, LLC
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

package com.comcast.money.core

import com.typesafe.config.Config

import java.lang.reflect.Modifier
import scala.reflect.ClassTag
import scala.util.{ Failure, Success, Try }

/**
 * Helper trait used to create plugin factories that can create instances of a plugin
 * from a common configuration format.
 *
 * Supports instantiating classes from the full canonical class name searching for public members
 * with the following precedence:
 *
 * 1. By static factory method that accepts a [[Config]] parameter: {{{
 *   object Service {
 *     def apply(config: Config): Service = { ... }
 *   }
 * }}}
 * 2. By a public constructor that accepts a [[Config]] parameter: {{{
 *   class Service {
 *     def this(config: Config) = { ... }
 *   }
 * }}}
 * 3. By a public parameterless constructor: {{{
 *   class Service {
 *     def this() = { ... }
 *   }
 * }}}
 * 4. By singleton instance by the specified name: {{{
 *   object Service { ... }
 * }}}
 *
 * @tparam T the trait of the plugin
 */
trait ConfigurableTypeFactory[T <: AnyRef] {
  private val TYPE_KEY: String = "type"
  private val CLASS_KEY: String = "class"
  private val APPLY_NAME: String = "apply"
  private val MODULE_SUFFIX: String = "$"
  private val MODULE_FIELD_NAME: String = "MODULE$"

  protected val tag: ClassTag[T]
  protected val knownTypes: PartialFunction[String, Config => T] = Map.empty

  def create(config: Seq[Config]): Try[Seq[T]] = {
    val seq = config.map(create)
    if (seq.isEmpty) Success(Seq.empty)
    else Try(seq.map { _.get })
  }

  def create(config: Config): Try[T] = {
    val mapping = if (config.hasPath(TYPE_KEY)) {
      knownTypeFactory(config.getString(TYPE_KEY))
    } else {
      classInstanceFactory(config.getString(CLASS_KEY))
    }

    mapping.flatMap(factory => factory(config))
  }

  protected def knownTypeFactory(typeName: String): Try[Config => Try[T]] =
    knownTypes.lift(typeName) match {
      case Some(mapping) => Success((conf: Config) => Try(mapping(conf)))
      case None => Failure(new Exception(s"Could not resolve ${tag.runtimeClass.getSimpleName} known type '$typeName'."))
    }

  protected def classInstanceFactory(className: String): Try[Config => Try[T]] = {
    val factory = findClass(className) match {
      case Success(cls) =>
        createInstanceFactory(cls)
      case Failure(ex) =>
        findModule(className).toOption.flatMap(findStaticInstance) match {
          case Some(instance) => Success((_: Config) => Success(instance))
          case None => Failure(ex)
        }
    }

    factory match {
      case Success(mapping) => Success(mapping)
      case Failure(cause) => Failure(new Exception(s"Could not create instance of ${tag.runtimeClass.getSimpleName} class '$className'.", cause))
    }
  }

  protected def createInstanceFactory(cls: Class[_]): Try[Config => Try[T]] =
    if (tag.runtimeClass.isAssignableFrom(cls)) {
      val factory = findFactoryMethod(cls)
        .orElse(findConfigConstructor(cls))
        .orElse(findDefaultConstructor(cls))

      factory match {
        case Some(mapping) => Success((conf: Config) => mapping(conf))
        case None => Failure(new Exception(s"Could not find acceptable constructor or factory method for class ${cls.getCanonicalName}."))
      }
    } else {
      Failure(new Exception(s"Class ${cls.getCanonicalName} is not derived from type ${tag.runtimeClass.getCanonicalName}."))
    }

  private def findClass(className: String): Try[Class[_]] =
    Try(Class.forName(className))

  private def findModule(moduleName: String): Try[Class[_]] =
    findClass(moduleName + MODULE_SUFFIX)

  private def findFactoryMethod(cls: Class[_]): Option[Config => Try[T]] =
    cls.getMethods.find {
      method =>
        Modifier.isStatic(method.getModifiers) &&
          method.getParameterCount == 1 &&
          method.getParameterTypes()(0) == classOf[Config] &&
          method.getReturnType == cls &&
          method.getName == APPLY_NAME
    }
      .map { method => config: Config => Try(method.invoke(null, config).asInstanceOf[T]) }

  private def findConfigConstructor(cls: Class[_]): Option[Config => Try[T]] =
    cls.getConstructors.find {
      constructor =>
        constructor.getParameterCount == 1 &&
          constructor.getParameterTypes()(0) == classOf[Config]
    }
      .map { constructor => config: Config => Try(constructor.newInstance(config).asInstanceOf[T]) }

  private def findDefaultConstructor(cls: Class[_]): Option[Config => Try[T]] =
    cls.getConstructors.find {
      _.getParameterCount == 0
    }
      .map { constructor => _: Config => Try(constructor.newInstance().asInstanceOf[T]) }

  private def findStaticInstance(cls: Class[_]): Option[T] =
    cls.getFields.find {
      field =>
        Modifier.isStatic(field.getModifiers) &&
          field.getType == cls &&
          field.getName == MODULE_FIELD_NAME
    }
      .map { field => field.get(null).asInstanceOf[T] }
}
