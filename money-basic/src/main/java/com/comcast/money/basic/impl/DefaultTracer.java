package com.comcast.money.basic.impl;

import com.comcast.money.basic.Note;
import com.comcast.money.basic.SpanId;
import com.comcast.money.basic.SpanService;
import com.comcast.money.basic.TraceContext;
import com.comcast.money.basic.Tracer;

public class DefaultTracer implements Tracer {

    private final TraceContext traceContext;
    private final SpanService spanService;

    public DefaultTracer(TraceContext traceContext, SpanService spanService) {
        this.traceContext = traceContext;
        this.spanService = spanService;
    }

    @Override
    public void startSpan(String spanName) {

        startSpan(spanName, false);
    }

    @Override
    public void startSpan(String spanName, boolean propagate) {

        SpanId current = traceContext.current();
        SpanId newSpanId;

        if (current != null) {
            newSpanId = current.newChild();
        } else {
            newSpanId = new SpanId();
        }
        traceContext.push(newSpanId);
        spanService.start(newSpanId, current, spanName, propagate);
    }

    @Override
    public void stopSpan(boolean result) {

        SpanId current = traceContext.pop();
        if (current != null) {
            spanService.stop(current, result);
        }
    }

    @Override
    public void stopSpan() {
        stopSpan(true);
    }

    @Override
    public void record(String key, String value) {
        SpanId current = traceContext.current();
        if (current != null) {
            spanService.record(current, new Note<String>(key, value));
        }
    }

    @Override
    public void record(String key, Boolean value) {
        SpanId current = traceContext.current();
        if (current != null) {
            spanService.record(current, new Note<Boolean>(key, value));
        }
    }

    @Override
    public void record(String key, Double value) {
        SpanId current = traceContext.current();
        if (current != null) {
            spanService.record(current, new Note<Double>(key, value));
        }
    }

    @Override
    public void record(String key, Long value) {
        SpanId current = traceContext.current();
        if (current != null) {
            spanService.record(current, new Note<Long>(key, value));
        }
    }

    @Override
    public void startTimer(String timerKey) {
        SpanId current = traceContext.current();
        if (current != null) {
            spanService.startTimer(current, timerKey);
        }
    }

    @Override
    public void stopTimer(String timerKey) {
        SpanId current = traceContext.current();
        if (current != null) {
            spanService.stopTimer(current, timerKey);
        }
    }
}
