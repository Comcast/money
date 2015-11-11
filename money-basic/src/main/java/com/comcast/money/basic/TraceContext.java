package com.comcast.money.basic;

public interface TraceContext {

    void push(SpanId spanId);

    SpanId pop();

    void clear();

    SpanId current();
}
