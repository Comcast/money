package com.comcast.money.basic;

public interface Span {

    void start(Long startTime, Span parentSpan, boolean propagate);

    /**
     * Ends a span, moving it to a Closing state
     * @param stopTime The time when the span ended (in milliseconds)
     * @param result The result of the span (success or failure)
     */
    void stop(Long stopTime, boolean result);

    /**
     * Different than end, this will shut down the span and have it emit it's data
     */
    void close();

    void record(Note<?> note);

    void startTimer(String timerKey, Long startTime);

    void stopTimer(String timerKey, Long endTime);

    void timedOut();

    SpanData data();

    boolean isTimedOut();

    boolean shouldClose();
}
