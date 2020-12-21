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

import com.typesafe.config.{ Config, ConfigFactory }
import org.scalatest.Inside.inside
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.reflect.ClassTag

class ConfigurableTypeFactorySpec extends AnyWordSpec with Matchers {

  "ConfigurableTypeFactorySpec" should {
    "creates a known type" in {
      val config = ConfigFactory.parseString(
        """
          | type = "known"
          |""".stripMargin)

      val result = ServiceFactory.create(config)

      inside(result) {
        case Some(s: KnownService) =>
          s.config shouldBe config
      }
    }

    "creates a singleton class" in {
      val config = ConfigFactory.parseString(
        s"""
          | class = "com.comcast.money.core.SingletonService"
          |""".stripMargin)

      val result = ServiceFactory.create(config)
      result shouldBe Some(SingletonService)
    }

    "creates a class with a factory method" in {
      val config = ConfigFactory.parseString(
        s"""
           | class = "${classOf[FactoryService].getCanonicalName}"
           | value = "value"
           |""".stripMargin)

      val result = ServiceFactory.create(config)
      inside(result) {
        case Some(s: FactoryService) =>
          s.value shouldBe "value"
      }
    }

    "creates a class with a config constructor" in {
      val config = ConfigFactory.parseString(
        s"""
           | class = "${classOf[ConfigConstructorService].getCanonicalName}"
           |""".stripMargin)

      val result = ServiceFactory.create(config)
      inside(result) {
        case Some(s: ConfigConstructorService) =>
          s.config shouldBe config
      }
    }

    "creates a class with a default constructor" in {
      val config = ConfigFactory.parseString(
        s"""
           | class = "${classOf[DefaultConstructorService].getCanonicalName}"
           |""".stripMargin)

      val result = ServiceFactory.create(config)
      inside(result) {
        case Some(_: DefaultConstructorService) =>
      }
    }

    "returns the default value for an unknown type" in {
      val config = ConfigFactory.parseString(
        s"""
           | type = "unknown"
           |""".stripMargin)

      val result = ServiceFactory.create(config)
      result shouldBe Some(DefaultService)
    }

    "returns the default value for an unknown class" in {
      val config = ConfigFactory.parseString(
        s"""
           | class = "com.comcast.money.core.UnknownService"
           |""".stripMargin)

      val result = ServiceFactory.create(config)
      result shouldBe Some(DefaultService)
    }
  }
}

object ServiceFactory extends ConfigurableTypeFactory[Service] {
  override protected val tag: ClassTag[Service] = ClassTag(classOf[Service])
  override protected val defaultValue: Option[Service] = Some(DefaultService)
  override protected val knownTypes: PartialFunction[String, Config => Service] = {
    case "known" => config => new KnownService(config)
  }
}

trait Service {}

object FactoryService {
  def apply(config: Config): FactoryService = new FactoryService(config.getString("value"))
}
class FactoryService(val value: String) extends Service {}

class ConfigConstructorService(val config: Config) extends Service {}
class DefaultConstructorService extends Service {}
class KnownService(val config: Config) extends Service {}
object SingletonService extends Service {}
object DefaultService extends Service {}