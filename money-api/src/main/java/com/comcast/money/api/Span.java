package com.comcast.money.api;

/**
 * A Span is a container that represents a single invocation of a traced service / operation.
 */
public interface Span {

    /**
     * Signals the span that it has started
     */
    void start();

    /**
     * Stops the span asserts a successful result
     */
    void stop();

    /**
     * Ends a span, moving it to a Stopped state
     * @param result The result of the span (success or failure)
     */
    void stop(boolean result);

    void record(String key, String value);

    void record(String key, Boolean value);

    void record(String key, Double value);

    void record(String key, Long value);

    void record(String key, String value, boolean propagate);

    void record(String key, Boolean value, boolean propagate);

    void record(String key, Double value, boolean propagate);

    void record(String key, Long value, boolean propagate);

    /**
     * Starts a timer
     *
     * @param timerKey The name of the timer, this will be used for the name of the note that is emitted
     */
    void startTimer(String timerKey);

    /**
     * Stops a timer
     *
     * @param timerKey The name of the timer to stop
     */
    void stopTimer(String timerKey);

    /**
     * @return The SpanId for this span
     */
    SpanId id();

    /**
     * @return The SpanData that is collected so far for this span
     */
    SpanData data();

    Long startTime();

    /**
     * Creates a new child from this span
     *
     * @param childName The name of the child to create
     * @param propagate True if the propagated notes from this span should be passed to the child
     * @return A new Span that is a child of this span
     */
    Span childSpan(String childName, boolean propagate);
}

