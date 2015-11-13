package com.comcast.money.core;

/**
 * A Span is a container that represents a single invocation of a traced service / operation.
 */
public interface Span {

    /**
     * Starts the Span.  All messages received before a span is started are ignored.
     *
     * @param startTime The time in MILLISECONDS when the span started
     * @param parentSpan The parent Span of the current span
     * @param propagate An indicator of whether we should propagate the observed notes of the parent to
     *                  the child span.
     */
    void start(Long startTime, Span parentSpan, boolean propagate);

    /**
     * Ends a span, moving it to a Stopped state
     * @param stopTime The time when the span ended in MILLISECONDS
     * @param result The result of the span (success or failure)
     */
    void stop(Long stopTime, boolean result);

    /**
     * Different than end, this will shut down the span and have it emit it's data
     */
    void close();

    /**
     * Records a Note
     * @param note A Note that contains some piece of data to tag this span with
     */
    void record(Note<?> note);

    /**
     * Starts a timer
     *
     * @param timerKey The name of the timer, this will be used for the name of the note that is emitted
     * @param startTime The time the timer started, this is an instant in MICROSECONDS
     */
    void startTimer(String timerKey, Long startTime);

    /**
     * Stops a timer
     *
     * @param timerKey The name of the timer to stop
     * @param endTime The end of the timer, this is an instant in MICROSECONDS
     */
    void stopTimer(String timerKey, Long endTime);

    /**
     * Times out a span; used when a span has been open for too long.
     *
     * This immediately stops and closes the span, emitting any data collected so far
     */
    void timedOut();

    /**
     * @return The SpanData that is collected so far for this span
     */
    SpanData data();

    // TODO: I worry about these concerns, should a Span be aware of when it timed out and if it should close, or someone else?
    /**
     * @return True if this span has timed out
     */
    boolean isTimedOut();

    /**
     * @return True if this span should be closed
     */
    boolean shouldClose();
}
