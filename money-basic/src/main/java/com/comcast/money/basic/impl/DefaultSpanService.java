package com.comcast.money.basic.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.comcast.money.basic.Note;
import com.comcast.money.basic.Span;
import com.comcast.money.basic.SpanEmitter;
import com.comcast.money.basic.SpanId;
import com.comcast.money.basic.SpanService;

public class DefaultSpanService implements SpanService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultSpanService.class);

    private final ConcurrentMap<SpanId, Span> spans = new ConcurrentHashMap<SpanId, Span>();
    private final ExecutorService executorService;
    private final SpanEmitter spanEmitter;
    private final ScheduledThreadPoolExecutor scheduler;

    public DefaultSpanService(ExecutorService executorService, SpanEmitter spanEmitter, ScheduledThreadPoolExecutor scheduler) {
        this.executorService = executorService;
        this.spanEmitter = spanEmitter;
        this.scheduler = scheduler;
    }

    @Override
    public void start(SpanId spanId, boolean parentInContext) {
        if (spans.containsKey(spanId)) {
            logger.warn("Cannot start span with id {}; it already exists.", spanId);
        } else {
            final Span span = spans.putIfAbsent(spanId, new DefaultSpan(spanId, spanEmitter));
            span.begin(System.nanoTime() / 1000);

            // TODO: make the timeout configurable, here it is hardcoded to 10 seconds
            scheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    Long endTime = System.currentTimeMillis();
                    boolean result = false;
                    span.end(endTime, result);
                }
            }, 10, TimeUnit.SECONDS);
        }
    }

    @Override
    public void stop(final SpanId spanId, final boolean result) {
        final Long stopTime = System.nanoTime() / 1000;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                if (!spans.containsKey(spanId)) {
                    logger.warn("Cannot stop span with id {}; it does not exist.", spanId);
                } else {
                    Span span = spans.remove(spanId);
                    span.end(stopTime, result);
                }
            }
        });
    }

    @Override
    public void record(final SpanId spanId, final Note<?> note) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                if (!spans.containsKey(spanId)) {
                    logger.warn("Cannot record span with id {}; it does not exist.", spanId);
                } else {
                    Span span = spans.get(spanId);
                    span.record(note);
                }
            }
        });
    }

    @Override
    public void startTimer(final SpanId spanId, final String timerKey) {
        final Long startTime = System.nanoTime() / 1000;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                if (!spans.containsKey(spanId)) {
                    logger.warn("Cannot record span with id {}; it does not exist.", spanId);
                } else {
                    Span span = spans.get(spanId);
                    span.startTimer(timerKey, startTime);
                }
            }
        });
    }

    @Override
    public void stopTimer(final SpanId spanId, final String timerKey) {
        final Long endTime = System.nanoTime() / 1000;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                if (!spans.containsKey(spanId)) {
                    logger.warn("Cannot record span with id {}; it does not exist.", spanId);
                } else {
                    Span span = spans.get(spanId);
                    span.stopTimer(timerKey, endTime);
                }
            }
        });
    }
}
