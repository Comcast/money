package com.comcast.money.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import com.comcast.money.core.impl.DefaultTracer;
import com.comcast.money.core.impl.SpanReaper;
import com.comcast.money.core.impl.ThreadLocalTraceContext;

public class Money {

    public final static Config config;
    public final static MoneySettings settings;
    public final static Tracer tracer;
    private final static ExecutorService moneyExecutor;
    private final static ScheduledExecutorService moneyScheduler;

    static {
        config = ConfigFactory.load();
        settings = new MoneySettings(config);

        moneyExecutor = Executors.newFixedThreadPool(settings.getExecutorSettings().getThreadCount());
        moneyScheduler = Executors.newScheduledThreadPool(settings.getSchedulerSettings().getThreadCount());

        if (settings.isEnabled()) {
            SpanEmitter emitter = SpanEmitters.load(config, moneyExecutor);
            SpanReaper reaper = new SpanReaper(moneyScheduler, settings.getReaperInterval());
            tracer = new DefaultTracer(new ThreadLocalTraceContext(), emitter, reaper, settings.getSpanTimeout(), settings.getStoppedSpanTimeout());
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
