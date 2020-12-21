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
import org.slf4j.{ Logger, LoggerFactory }

import java.lang.reflect.Modifier
import scala.reflect.ClassTag
import scala.util.Try

trait ConfigurableTypeFactory[T <: AnyRef] {
  private val TYPE_KEY: String = "type"
  private val CLASS_KEY: String = "class"
  private val APPLY_NAME: String = "apply"
  private val MODULE_SUFFIX: String = "$"
  private val MODULE_FIELD_NAME: String = "MODULE$"

  protected val tag: ClassTag[T]
  protected val logger: Logger = LoggerFactory.getLogger(getClass)
  protected val knownTypes: PartialFunction[String, Config => T] = Map.empty
  protected val defaultValue: Option[T] = None

  def create(config: Config): Option[T] =
    findConfigValue(config, TYPE_KEY)
      .flatMap(findKnownType)
      .map(_(config))
      .orElse(findConfigValue(config, CLASS_KEY)
        .flatMap(createInstance(_, config)))
      .orElse(defaultValue)

  protected def findConfigValue(config: Config, key: String): Option[String] =
    if (config.hasPath(key)) {
      Option(config.getString(key))
    } else {
      None
    }

  protected def findKnownType(typeName: String): Option[Config => T] =
    knownTypes.lift(typeName)
      .orElse({
        logger.warn(s"Could not find ${tag.runtimeClass.getSimpleName} type: '$typeName'")
        None
      })

  protected def createInstance(className: String, config: Config): Option[T] = for {
    factory <- findConfigurableTypeFactory(className)
    instance = factory(config)
  } yield instance

  protected def findConfigurableTypeFactory(className: String): Option[Config => T] =
    findClass(className)
      .filter(tag.runtimeClass.isAssignableFrom)
      .flatMap({
        classInstance =>
          findFactoryMethod(classInstance)
            .orElse(findConfigConstructor(classInstance))
            .orElse(findDefaultConstructor(classInstance))
      })
      .orElse(findModule(className)
        .flatMap({
          moduleInstance => findStaticInstance(moduleInstance)
        }))
      .orElse({
        logger.warn(s"Could not create ${tag.runtimeClass.getSimpleName} class: '$className'")
        None
      })

  private def findClass(className: String): Option[Class[_]] =
    Try(Class.forName(className)).toOption

  private def findModule(moduleName: String): Option[Class[_]] =
    findClass(moduleName + MODULE_SUFFIX)

  private def findFactoryMethod(cls: Class[_]): Option[Config => T] =
    cls.getMethods.find {
      method =>
        Modifier.isStatic(method.getModifiers) &&
          method.getParameterCount == 1 &&
          method.getParameterTypes()(0) == classOf[Config] &&
          method.getReturnType == cls &&
          method.getName == APPLY_NAME
    } map { method => config: Config => method.invoke(null, config).asInstanceOf[T] }

  private def findConfigConstructor(cls: Class[_]): Option[Config => T] =
    cls.getConstructors.find {
      constructor =>
        constructor.getParameterCount == 1 &&
          constructor.getParameterTypes()(0) == classOf[Config]
    } map { constructor => config: Config => constructor.newInstance(config).asInstanceOf[T] }

  private def findDefaultConstructor(cls: Class[_]): Option[Config => T] =
    cls.getConstructors.find {
      _.getParameterCount == 0
    } map { constructor => _: Config => constructor.newInstance().asInstanceOf[T] }

  private def findStaticInstance(cls: Class[_]): Option[Config => T] =
    cls.getFields.find {
      field =>
        Modifier.isStatic(field.getModifiers) &&
          field.getType == cls &&
          field.getName == MODULE_FIELD_NAME
    } map { field => _: Config => field.get(null).asInstanceOf[T] }
}
