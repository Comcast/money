package com.comcast.money.core;

public interface SpanService {

    void start(SpanId spanId, SpanId parentSpanId, String spanName, boolean propagate);

    void stop(SpanId spanId, boolean result);

    void record(SpanId spanId, Note<?> note);

    void startTimer(SpanId spanId, String timerKey);

    void stopTimer(SpanId spanId, String timerKey);
}
