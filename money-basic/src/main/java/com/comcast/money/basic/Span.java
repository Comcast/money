package com.comcast.money.basic;

public interface Span {

    void begin(Long startTime, Span parentSpan, boolean propagate);

    void end(Long endTime, boolean result);

    void record(Note<?> note);

    void startTimer(String timerKey, Long startTime);

    void stopTimer(String timerKey, Long endTime);

    void timedOut();

    SpanData data();

    boolean isExpired();
}
