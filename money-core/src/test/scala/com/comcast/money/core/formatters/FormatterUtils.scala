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

package com.comcast.money.core.formatters

import java.util.{ Locale, UUID }

object FormatterUtils {
  def isValidIds(traceId: UUID, spanId: Long): Boolean =
    (traceId.getLeastSignificantBits != 0 || traceId.getMostSignificantBits != 0) && spanId != 0

  def isValidIds(traceId: UUID, parentSpanId: Long, spanId: Long): Boolean =
    (traceId.getLeastSignificantBits != 0 || traceId.getMostSignificantBits != 0) && parentSpanId != 0 && spanId != 0

  implicit class StringWithHexHeaderConversion(s: String) {
    def fromHexStringToLong: Long = java.lang.Long.parseUnsignedLong(s, 16)

    def toGuid: String = {
      val padded = if (s.length < 32) {
        (("0" * 32) + s).substring(0, 32)
      } else {
        s
      }
      String.format(
        "%s-%s-%s-%s-%s",
        padded.substring(0, 8),
        padded.substring(8, 12),
        padded.substring(12, 16),
        padded.substring(16, 20),
        padded.substring(20, 32))
    }

    def fromGuid: String = s.replace("-", "").toLowerCase
  }

  implicit class LongToHexConversion(l: Long) {
    def hex64: String = f"$l%016x"
  }

  implicit class UUIDToHexConversion(uuid: UUID) {
    def id: String = uuid.toString.toLowerCase(Locale.US)

    def hex128: String = uuid.getMostSignificantBits.hex64 + uuid.getLeastSignificantBits.hex64

    def hex64or128: String = if (uuid.getMostSignificantBits == 0) {
      uuid.getLeastSignificantBits.hex64
    } else {
      hex128
    }
  }

}
