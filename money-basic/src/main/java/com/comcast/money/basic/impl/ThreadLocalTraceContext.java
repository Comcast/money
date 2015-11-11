package com.comcast.money.basic.impl;

import java.util.Stack;

import com.comcast.money.basic.SpanId;
import com.comcast.money.basic.TraceContext;

public class ThreadLocalTraceContext implements TraceContext {

    private static InheritableThreadLocal<Stack<SpanId>> threadLocalCtx = new InheritableThreadLocal<Stack<SpanId>>();

    static {
        threadLocalCtx.set(new Stack<SpanId>());
    }

    public SpanId current() {
        return stack().peek();
    }

    public void push(SpanId spanId) {
        stack().push(spanId);
    }

    public SpanId pop() {
        return stack().pop();
    }

    public void clear() {
        stack().clear();
    }

    private Stack<SpanId> stack() {
        return threadLocalCtx.get();
    }
}
