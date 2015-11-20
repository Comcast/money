package com.comcast.money.core.concurrent;

import java.util.Map;
import java.util.concurrent.Executor;

import org.slf4j.MDC;

import com.comcast.money.core.Span;
import com.comcast.money.core.TraceContext;
import com.comcast.money.core.impl.MDCSupport;

public class TraceFriendlyExecutor implements Executor {

    private final TraceContext traceContext;
    private final Executor wrapped;
    private final MDCSupport mdcSupport;

    public TraceFriendlyExecutor(Executor wrapped, TraceContext traceContext, MDCSupport mdcSupport) {
        this.wrapped = wrapped;
        this.traceContext = traceContext;
        this.mdcSupport = mdcSupport;
    }

    /**
     * This is the only method that we need to override; we need to inject the current trace context
     * into the runnable that is being run here
     */
    @Override
    public void execute(final Runnable command) {
        final Span submittingThreadsSpan = traceContext.current();
        final Map submittingThreadsContext = MDC.getCopyOfContextMap();

        wrapped.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    mdcSupport.propagate(submittingThreadsContext);
                    traceContext.clear();
                    if (submittingThreadsSpan != null) {
                        traceContext.push(submittingThreadsSpan);
                    }
                    command.run();
                } finally {
                    traceContext.clear();
                    mdcSupport.clear();
                }
            }
        });
    }
}