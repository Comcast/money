package com.comcast.money.core;

/**
 * A Span is a container that represents a single invocation of a traced service / operation.
 */
public interface Span {

    /**
     * Signals the span that it has started
     */
    void start();

    /**
     * Ends a span, moving it to a Stopped state
     * @param result The result of the span (success or failure)
     */
    void stop(boolean result);

    /**
     * Different than stop, this will shut down the span and have it emit it's data
     *
     * A span is considered "done" after it is closed
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
     */
    void startTimer(String timerKey);

    /**
     * Stops a timer
     *
     * @param timerKey The name of the timer to stop
     */
    void stopTimer(String timerKey);

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

    /**
     * @return True if this span has timed out
     */
    boolean isTimedOut();

    /**
     * @return True if this span should be closed
     */
    boolean shouldClose();

    /**
     * Creates a new child from this span
     * @param childName The name of the child to create
     * @param propagate True if the notes from the parent should be passed to the child
     * @return A new Span that is a child of this
     */
    Span newChild(String childName, boolean propagate);
}
