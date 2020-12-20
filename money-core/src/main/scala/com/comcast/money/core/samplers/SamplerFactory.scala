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

package com.comcast.money.core.samplers

import com.comcast.money.core.ConfigurableTypeFactory
import com.typesafe.config.Config

import scala.reflect.ClassTag

object SamplerFactory extends ConfigurableTypeFactory[Sampler] {
  override protected val tag: ClassTag[Sampler] = ClassTag(classOf[Sampler])
  override protected val defaultValue: Option[Sampler] = Some(AlwaysOnSampler)

  override protected val knownTypes: PartialFunction[String, Config => Sampler] = {
    case "always-on" => _ => AlwaysOnSampler
    case "always-off" => _ => AlwaysOffSampler
    case "ratio-based" => config => RatioBasedSampler(config)
    case "parent-based" => config => ParentBasedSampler(config)
  }
}
