/*
 * Copyright 2012-2015 Comcast Cable Communications Management, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.comcast.money.core.impl;

import java.util.Stack;

import com.comcast.money.core.Span;
import com.comcast.money.core.TraceContext;

public class ThreadLocalTraceContext implements TraceContext {

    private static InheritableThreadLocal<Stack<Span>> threadLocalCtx = new InheritableThreadLocal<Stack<Span>>();

    static {
        threadLocalCtx.set(new Stack<Span>());
    }

    private final MDCSupport mdcSupport;

    public ThreadLocalTraceContext(MDCSupport mdcSupport) {
        this.mdcSupport = mdcSupport;
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
        mdcSupport.setSpan(span);
    }

    public Span pop() {

        Span result = stack().pop();

        // reset the mdc to the prior item in the thread, if null mdc will be cleared
        mdcSupport.setSpan
                (current());

        return result;
    }

    public void clear() {

        stack().clear();
        mdcSupport.setSpan(null);
    }

    private Stack<Span> stack() {
        return threadLocalCtx.get();
    }
}
