package com.comcast.money.core.impl;

import com.comcast.money.core.Note;
import com.comcast.money.core.Span;
import com.comcast.money.core.SpanEmitter;
import com.comcast.money.core.SpanId;
import com.comcast.money.core.TraceContext;
import com.comcast.money.core.Tracer;

public class DefaultTracer implements Tracer {

    private final TraceContext traceContext;
    private final SpanEmitter spanEmitter;
    private final SpanReaper spanReaper;
    private final long spanTimeout;
    private final long stoppedSpanTimeout;

    public DefaultTracer(TraceContext traceContext, SpanEmitter spanEmitter, SpanReaper spanReaper, long spanTimeout, long stoppedSpanTimeout) {
        this.traceContext = traceContext;
        this.spanEmitter = spanEmitter;
        this.spanTimeout = spanTimeout;
        this.stoppedSpanTimeout = stoppedSpanTimeout;
        this.spanReaper = spanReaper;
    }

    @Override
    public void startSpan(String spanName) {

        startSpan(spanName, false);
    }

    @Override
    public void startSpan(String spanName, boolean propagate) {

        Span current = traceContext.current();
        Span newSpan;

        if (current != null) {
            newSpan = current.newChild(spanName, propagate);
        } else {
            newSpan = new DefaultSpan(new SpanId(), spanName, spanEmitter, spanTimeout, stoppedSpanTimeout);
        }
        newSpan.start();
        traceContext.push(newSpan);
        spanReaper.watch(newSpan);
    }

    @Override
    public void stopSpan(boolean result) {

        Span current = traceContext.pop();
        if (current != null) {
            current.stop(result);
            spanReaper.unwatch(current);
        }
    }

    @Override
    public void stopSpan() {
        stopSpan(true);
    }

    @Override
    public void record(String key, String value) {
        Span current = traceContext.current();
        if (current != null) {
            current.record(new Note<String>(key, value));
        }
    }

    @Override
    public void record(String key, Boolean value) {
        Span current = traceContext.current();
        if (current != null) {
            current.record(new Note<Boolean>(key, value));
        }
    }

    @Override
    public void record(String key, Double value) {
        Span current = traceContext.current();
        if (current != null) {
            current.record(new Note<Double>(key, value));
        }
    }

    @Override
    public void record(String key, Long value) {
        Span current = traceContext.current();
        if (current != null) {
            current.record(new Note<Long>(key, value));
        }
    }

    @Override
    public void startTimer(String timerKey) {
        Span current = traceContext.current();
        if (current != null) {
            current.startTimer(timerKey);
        }
    }

    @Override
    public void stopTimer(String timerKey) {
        Span current = traceContext.current();
        if (current != null) {
            current.stopTimer(timerKey);
        }
    }
}
