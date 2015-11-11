package com.comcast.money.basic;

public interface SpanService {

    void start(SpanId spanId, boolean parentInContext);

    void stop(SpanId spanId, boolean result);

    void record(SpanId spanId, Note<?> note);

    void startTimer(SpanId spanId, String timerKey);

    void stopTimer(SpanId spanId, String timerKey);
}
