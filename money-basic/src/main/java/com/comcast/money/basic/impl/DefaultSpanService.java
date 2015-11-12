package com.comcast.money.basic.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.comcast.money.basic.Note;
import com.comcast.money.basic.Span;
import com.comcast.money.basic.SpanEmitter;
import com.comcast.money.basic.SpanId;
import com.comcast.money.basic.SpanService;

import static com.comcast.money.basic.TimeUtils.thisInstant;

public class DefaultSpanService implements SpanService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultSpanService.class);

    private final ConcurrentMap<SpanId, Span> spans = new ConcurrentHashMap<SpanId, Span>();
    private final ExecutorService executorService;
    private final SpanEmitter spanEmitter;
    private final ScheduledExecutorService scheduler;
    private final SpanReaper spanReaper;
    private final long spanTimeout;
    private final long reaperInterval;

    public DefaultSpanService(ExecutorService executorService, SpanEmitter spanEmitter, ScheduledExecutorService scheduler) {
        this.executorService = executorService;
        this.spanEmitter = spanEmitter;
        this.scheduler = scheduler;
        this.spanTimeout = 1000L; // TODO: make configurable

        // TODO: make these values configurable
        this.reaperInterval = 100L;
        this.spanReaper = new SpanReaper();

        // Schedules the cleanup of spans that expired
        this.scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                spanReaper.reap();
            }
        }, 100L, reaperInterval, TimeUnit.MILLISECONDS);
    }

    @Override
    public void start(SpanId spanId, SpanId parentSpanId, String spanName, boolean propagate) {
        if (spans.containsKey(spanId)) {
            logger.warn("Cannot start span with id {}; it already exists.", spanId);
        } else {
            Span newSpan = new DefaultSpan(spanId, spanName, spanEmitter, spanTimeout, reaperInterval);
            Span existingSpan = spans.putIfAbsent(spanId, newSpan);
            if (existingSpan != null) {
                logger.warn("Span with id {} was already found, possible concurrency issue!", spanId);
                newSpan = existingSpan;
            }

            if (parentSpanId != null) {
                final Span parentSpan = spans.get(parentSpanId);
                newSpan.start(System.currentTimeMillis(), parentSpan, propagate);
            } else {
                newSpan.start(System.currentTimeMillis(), null, false);
            }
            spanReaper.watch(newSpan);
        }
    }

    @Override
    public void stop(final SpanId spanId, final boolean result) {
        final Long stopTime = System.currentTimeMillis();
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                if (!spans.containsKey(spanId)) {
                    logger.warn("Cannot stop span with id {}; it does not exist.", spanId);
                } else {
                    Span span = spans.remove(spanId);
                    spanReaper.unwatch(span);
                    span.stop(stopTime, result);
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
        final Long startTime = thisInstant();
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
        final Long endTime = thisInstant();
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
