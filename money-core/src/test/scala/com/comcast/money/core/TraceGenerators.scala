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

package com.comcast.money.core

import java.time.Instant
import java.util.UUID

import org.scalacheck.{ Arbitrary, Gen }

import scala.concurrent.duration._
import scala.language.higherKinds
import Arbitrary.arbitrary
import com.comcast.money.api.{ Note, Span, SpanId }
import org.scalacheck.Gen.{ alphaLowerChar, alphaUpperChar, choose, frequency }

trait TraceGenerators {

  def genOption[A](g: Gen[A]): Gen[Option[A]] = Gen.oneOf(Gen.const(Option.empty[A]), g map Option.apply)

  def genUUID: Gen[UUID] = for {
    hi <- arbitrary[Long]
    lo <- arbitrary[Long]
  } yield new UUID(hi, lo)

  def genStr: Gen[String] = Gen.listOf(Gen.alphaNumChar) map {
    _.mkString
  }

  def genHexStrFromLong: Gen[String] = for {
    l <- arbitrary[Long]
  } yield l.toHexString

  def genTraceSystemMetadataPair: Gen[(String, String)] = for {
    name <- genStr
    value <- genStr
  } yield name -> value

  def genTraceSystemMetadata: Gen[Map[String, String]] = Gen.nonEmptyMap(genTraceSystemMetadataPair)

  def genSpanId: Gen[SpanId] = for {
    traceId <- genUUID
    parentId <- arbitrary[Long]
    childId <- arbitrary[Long]
  } yield new SpanId(traceId.toString, parentId, childId)

}

