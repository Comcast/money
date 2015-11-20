package com.comcast.money.spring3;

import org.springframework.stereotype.Component;

import com.comcast.money.core.Money;
import com.comcast.money.core.Note;
import com.comcast.money.core.Span;
import com.comcast.money.core.SpanId;
import com.comcast.money.core.Tracer;

@Component
public class SpringTracer implements Tracer {

    private final Tracer wrapped;

    public SpringTracer(Tracer wrapped) {
        this.wrapped = wrapped;
    }

    public SpringTracer() {
        this.wrapped = Money.tracer;
    }

    @Override
    public void setTraceContext(SpanId spanId) {
        wrapped.setTraceContext(spanId);
    }

    @Override
    public Span startSpan(String spanName) {
        return wrapped.startSpan(spanName);
    }

    @Override
    public Span startSpan(String spanName, boolean propagate) {
        return wrapped.startSpan(spanName, propagate);
    }

    @Override
    public void stopSpan(boolean result) {
        wrapped.stopSpan(result);
    }

    @Override
    public void stopSpan() {
        wrapped.stopSpan();
    }

    @Override
    public void record(String key, String value) {
        wrapped.record(key, value);
    }

    @Override
    public void record(String key, Boolean value) {
        wrapped.record(key, value);
    }

    @Override
    public void record(String key, Double value) {
        wrapped.record(key, value);
    }

    @Override
    public void record(String key, Long value) {
        wrapped.record(key, value);
    }

    @Override
    public void record(Note<?> note) {
        wrapped.record(note);
    }

    @Override
    public void startTimer(String timerKey) {
        wrapped.startTimer(timerKey);
    }

    @Override
    public void stopTimer(String timerKey) {
        wrapped.stopTimer(timerKey);
    }

    @Override
    public void record(String key, String value, boolean propagate) {
        wrapped.record(key, value, propagate);
    }

    @Override
    public void record(String key, Boolean value, boolean propagate) {
        wrapped.record(key, value, propagate);
    }

    @Override
    public void record(String key, Double value, boolean propagate) {
        wrapped.record(key, value, propagate);
    }

    @Override
    public void record(String key, Long value, boolean propagate) {
        wrapped.record(key, value, propagate);
    }
}
