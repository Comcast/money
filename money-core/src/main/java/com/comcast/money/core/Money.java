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
        System.out.println("\r\nLOADING MONEY");
        //System.out.println(config.root().render());
        if (settings.isEnabled()) {
            SpanEmitter emitter = SpanEmitters.load(config, moneyExecutor);
            SpanReaper reaper = new SpanReaper(moneyScheduler, settings.getReaperInterval());
            tracer = new DefaultTracer(new ThreadLocalTraceContext(), emitter, reaper, settings.getSpanTimeout(), settings.getStoppedSpanTimeout());
        } else {
            System.out.println("money disabled!!!!");
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
                public void record(Note<?> note) {
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
