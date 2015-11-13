package com.comcast.money.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import com.comcast.money.core.impl.DefaultTracer;
import com.comcast.money.core.impl.SpanReaper;
import com.comcast.money.core.impl.ThreadLocalTraceContext;

public class Money {

    public final static Config config = ConfigFactory.load();
    public final static Tracer tracer;
    private final static ExecutorService moneyExecutor;
    private final static ScheduledExecutorService moneyScheduler;

    static {
        moneyExecutor = Executors.newFixedThreadPool(config.getInt("money.executor.thread-count"));
        moneyScheduler = Executors.newScheduledThreadPool(config.getInt("money.scheduler.thread-count"));
        long spanTimeout = config.getDuration("money.span-timeout", TimeUnit.MILLISECONDS);
        long stoppedSpanTimeout = config.getDuration("money.stopped-span-timeout", TimeUnit.MILLISECONDS);
        long reaperInterval = config.getDuration("money.reaper-interval", TimeUnit.MILLISECONDS);

        if (config.getBoolean("money.enabled")) {
            SpanEmitter emitter = SpanEmitters.load(config, moneyExecutor);
            SpanReaper reaper = new SpanReaper(moneyScheduler, reaperInterval);
            tracer = new DefaultTracer(new ThreadLocalTraceContext(), emitter, reaper, spanTimeout, stoppedSpanTimeout);
        } else {
            // NO-OP
            tracer = new Tracer() {
                @Override
                public void startSpan(String spanName) {
                }

                @Override
                public void startSpan(String spanName, boolean propagate) {
                }

                @Override
                public void stopSpan(boolean result) {
                }

                @Override
                public void stopSpan() {
                }

                @Override
                public void record(String key, String value) {
                }

                @Override
                public void record(String key, Boolean value) {
                }

                @Override
                public void record(String key, Double value) {
                }

                @Override
                public void record(String key, Long value) {
                }

                @Override
                public void startTimer(String timerKey) {
                }

                @Override
                public void stopTimer(String timerKey) {
                }
            };
        }
    }
}
