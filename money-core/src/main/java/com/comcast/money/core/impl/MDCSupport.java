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

    public void setSpanMDC(Span span) {
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

    public void propagateMDC(Map contextMap) {
        if (enabled) {
            if (contextMap == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(contextMap);
            }
        }
    }
}
