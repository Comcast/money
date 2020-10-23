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

package com.comcast.money.api;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IdGenerator {
    private IdGenerator() { }

    public static final String INVALID_TRACE_ID = "00000000-0000-0000-0000-000000000000";
    public static final String INVALID_TRACE_ID_HEX = "00000000000000000000000000000000";
    public static final long INVALID_ID = 0L;
    public static final String INVALID_ID_HEX = "0000000000000000";

    private static final Pattern TRACE_ID_PATTERN = Pattern.compile("^([0-9a-f]{8})-?([0-9a-f]{4})-([0-9a-f]{4})-?([0-9a-f]{4})-?([0-9a-f]{12})$", Pattern.CASE_INSENSITIVE);
    private static final Pattern TRACE_ID_HEX_PATTERN = Pattern.compile("^(?:[0-9a-f]{16}){1,2}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ID_HEX_PATTERN = Pattern.compile("^[0-9a-f]{16}$", Pattern.CASE_INSENSITIVE);

    /**
     * Generates a random trace ID.
     */
    public static String generateRandomTraceId() {
        long hi, lo;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        do {
            hi = random.nextLong();
            lo = random.nextLong();
        } while (hi == INVALID_ID && lo == INVALID_ID);
        return new UUID(hi, lo).toString();
    }

    /**
     * Generates a random non-zero 64-bit ID that can be used as span ID
     */
    public static long generateRandomId() {
        long id;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        do {
            id = random.nextLong();
        } while (id == INVALID_ID);
        return id;
    }

    public static String generateRandomTraceIdAsHex() {
        long hi, lo;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        do {
            hi = random.nextLong();
            lo = random.nextLong();
        } while (hi == INVALID_ID && lo == INVALID_ID);
        return convertIdToHex(hi) + convertIdToHex(lo);
    }

    public static String generateRandomIdAsHex() {
        return convertIdToHex(generateRandomId());
    }

    public static String convertTraceIdToHex(String traceId) {
        if (traceId != null) {
            Matcher matcher = TRACE_ID_PATTERN.matcher(traceId);
            if (matcher.matches()) {
                return matcher.replaceFirst("$1$2$3$4$5")
                        .toLowerCase(Locale.US);
            }
        }
        return INVALID_TRACE_ID_HEX;
    }

    public static String convertIdToHex(long id) {
        return io.opentelemetry.trace.SpanId.fromLong(id);
    }

    public static boolean isValidTraceId(String traceId) {
        return traceId != null && TRACE_ID_PATTERN.matcher(traceId).matches();
    }

    public static boolean isValidHexTraceId(String hex) {
        return hex != null && TRACE_ID_HEX_PATTERN.matcher(hex).matches();
    }

    /**
     * Parses a 64-bit or 128-bit hexadecimal string into a trace ID.
     */
    public static String parseTraceIdFromHex(String hex) {
        if (hex != null) {
            Matcher matcher = TRACE_ID_HEX_PATTERN.matcher(hex);
            if (matcher.matches()) {
                if (hex.length() == 16) {
                    hex = INVALID_ID_HEX + hex;
                }
                return new StringBuilder(36)
                        .append(hex.subSequence(0, 8))
                        .append('-')
                        .append(hex.subSequence(8, 12))
                        .append('-')
                        .append(hex.subSequence(12, 16))
                        .append('-')
                        .append(hex.subSequence(16, 20))
                        .append('-')
                        .append(hex.subSequence(20, 32))
                        .toString()
                        .toLowerCase(Locale.US);
            }
        }
        return INVALID_TRACE_ID;
    }

    /**
     * Parses a 64-bit hexadecimal string into a numeric ID.
     */
    public static long parseIdFromHex(String hex) {
        if (hex != null) {
            Matcher matcher = ID_HEX_PATTERN.matcher(hex);
            if (matcher.matches()) {
                return Long.parseUnsignedLong(hex, 16);
            }
        }
        return INVALID_ID;
    }
}
