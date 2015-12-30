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

package com.comcast.money.internal

import akka.actor._
import com.comcast.money.akka.ActorMaker
import com.comcast.money.api.SpanId
import com.comcast.money.internal.SpanFSMProtocol.{ PropagateNotesRequest, SpanCommand, Start }

object SpanSupervisorProtocol {

  case class SpanMessage(spanId: SpanId, command: SpanCommand)
}

object SpanSupervisor {

  def props(emitterSupervisorRef: ActorRef): Props = {
    Props(classOf[SpanSupervisor], emitterSupervisorRef)
  }
}

class SpanSupervisor(emitterSupervisorRef: ActorRef) extends Actor
    with ActorMaker with ActorLogging {

  import SpanSupervisorProtocol._

  def receive = {

    case SpanMessage(spanId: SpanId, message: Start) =>
      val spanFSMRef = makeActor(SpanFSM.props(emitterSupervisorRef), spanId.toString)
      spanFSMRef ! message

    case SpanMessage(spanId: SpanId, message: SpanCommand) =>
      context.child(spanId.toString) match {
        case Some(spanFSMRef) =>
          spanFSMRef forward message
        case None =>
          if (!message.isInstanceOf[PropagateNotesRequest])
            log.warning("Attempted to message non-existent SpanFSM for spanId {}, w/ message: {}", spanId, message)
      }
  }
}
