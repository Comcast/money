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

package com.comcast.money.akka.stream

trait PushConfig[In, Out] {
  val key: String
  val pushLogic: In => (Either[Unit, Out], Boolean)
  val tracingDSLUsage: TracingDSLUsage
}

trait StatefulPusher[In, Out] {
  def push(in: In): (Either[Unit, Out], Boolean)
}

case class StatefulPushConfig[In, Out](key: String, statefulPusher: StatefulPusher[In, Out], tracingDSLUsage: TracingDSLUsage) extends PushConfig[In, Out] {
  override val pushLogic: In => (Either[Unit, Out], Boolean) = statefulPusher.push
}

case class StatelessPushConfig[In, Out](key: String, pushLogic: In => (Either[Unit, Out], Boolean), tracingDSLUsage: TracingDSLUsage) extends PushConfig[In, Out]

trait TracingDSLUsage

case object UsingTracingDSL extends TracingDSLUsage

case object NotUsingTracingDSL extends TracingDSLUsage
