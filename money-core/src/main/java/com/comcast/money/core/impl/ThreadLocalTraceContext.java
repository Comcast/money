package com.comcast.money.core.impl;

import java.util.Stack;

import com.comcast.money.core.Span;
import com.comcast.money.core.TraceContext;

public class ThreadLocalTraceContext implements TraceContext {

    private static InheritableThreadLocal<Stack<Span>> threadLocalCtx = new InheritableThreadLocal<Stack<Span>>();

    static {
        threadLocalCtx.set(new Stack<Span>());
    }

    public Span current() {
        if (!stack().isEmpty()) {
            return stack().peek();
        } else {
            return null;
        }
    }

    public void push(Span span) {
        stack().push(span);
    }

    public Span pop() {
        return stack().pop();
    }

    public void clear() {
        stack().clear();
    }

    private Stack<Span> stack() {
        return threadLocalCtx.get();
    }
}
