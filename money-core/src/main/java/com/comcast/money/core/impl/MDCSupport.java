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

package com.comcast.money.core.impl;

import java.util.Map;

import org.slf4j.MDC;

import com.comcast.money.core.Span;
import com.comcast.money.core.SpanData;
import com.comcast.money.core.SpanId;

public class MDCSupport {

    private static final String MONEY_TRACE_KEY = "moneyTrace";
    private static final String MONEY_TRACE_FORMAT = "[ span-id=%s ][ trace-id=%s ][ parent-id=%s ]";
    private static final String SPAN_NAME_KEY = "spanName";

    private final boolean enabled;

    public MDCSupport(boolean enabled) {
        this.enabled = enabled;
    }

    public void setSpan(Span span) {
        if (enabled) {
            if (span == null) {
                MDC.remove(MONEY_TRACE_KEY);
                MDC.remove(SPAN_NAME_KEY);
            } else {
                SpanData data = span.data();
                SpanId spanId = data.getSpanId();
                String name = data.getName();
                MDC.put(MONEY_TRACE_KEY, String.format(MONEY_TRACE_FORMAT, spanId.getSelfId(), spanId.getTraceId(), spanId.getParentId()));
                MDC.put(SPAN_NAME_KEY, name);
            }
        }
    }

    public void propagate(Map contextMap) {
        if (enabled) {
            if (contextMap == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(contextMap);
            }
        }
    }

    public void clear() {
        MDC.clear();
    }

    public String getSpanName() {
        return MDC.get(SPAN_NAME_KEY);
    }
}
