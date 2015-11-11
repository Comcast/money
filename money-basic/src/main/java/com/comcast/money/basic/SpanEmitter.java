package com.comcast.money.basic;

public interface SpanEmitter {

    /**
     * Handles span data, this should be non-blocking
     * @param spanData representation of a span
     */
    void emit(SpanData spanData);
}
