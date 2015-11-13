package com.comcast.money.core;

public interface TraceContext {

    void push(Span span);

    Span pop();

    void clear();

    Span current();
}
