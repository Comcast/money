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

package com.comcast.money.core;

import java.util.List;
import java.util.Map;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import lombok.Builder;
import lombok.Value;

import com.comcast.money.api.EventInfo;
import com.comcast.money.api.InstrumentationLibrary;
import com.comcast.money.api.LinkInfo;
import com.comcast.money.api.Note;
import com.comcast.money.api.SpanId;
import com.comcast.money.api.SpanInfo;

@Value
@Builder(toBuilder = true)
public class CoreSpanInfo implements SpanInfo {
    String appName;
    String host;
    InstrumentationLibrary library;
    SpanId id;
    String name;
    SpanKind kind;
    List<LinkInfo> links;
    long startTimeNanos;
    long endTimeNanos;
    boolean hasEnded;
    long durationNanos;
    StatusCode status;
    String description;
    Map<String, Note<?>> notes;
    List<EventInfo> events;

    @Override
    public boolean isRecording() {
        return true;
    }
}
