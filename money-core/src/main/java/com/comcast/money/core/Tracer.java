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

package com.comcast.money.core;

public interface Tracer {

    void setTraceContext(SpanId spanId);

    Span startSpan(String spanName);

    Span startSpan(String spanName, boolean propagate);

    void stopSpan(boolean result);

    void stopSpan();

    void record(String key, String value);

    void record(String key, Boolean value);

    void record(String key, Double value);

    void record(String key, Long value);

    void record(Note<?> note);

    void record(String key, String value, boolean propagate);

    void record(String key, Boolean value, boolean propagate);

    void record(String key, Double value, boolean propagate);

    void record(String key, Long value, boolean propagate);

    void startTimer(String timerKey);

    void stopTimer(String timerKey);
}
