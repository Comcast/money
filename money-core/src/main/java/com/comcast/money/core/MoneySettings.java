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

import java.util.concurrent.TimeUnit;

import com.typesafe.config.Config;

public class MoneySettings {

    public static class ExecutorSettings {
        private final int threadCount;

        public ExecutorSettings(Config config) {
            this(config.getInt("thread-count"));
        }

        public ExecutorSettings(int threadCount) {
            this.threadCount = threadCount;
        }

        public int getThreadCount() {
            return threadCount;
        }
    }

    public static class SchedulerSettings {
        private final int threadCount;

        public SchedulerSettings(Config config) {
            this(config.getInt("thread-count"));
        }

        public SchedulerSettings(int threadCount) {
            this.threadCount = threadCount;
        }

        public int getThreadCount() {
            return threadCount;
        }
    }

    private final String appName;
    private final boolean enabled;
    private final long spanTimeout;
    private final long stoppedSpanTimeout;
    private final long reaperInterval;
    private final ExecutorSettings executorSettings;
    private final SchedulerSettings schedulerSettings;

    public MoneySettings(Config config) {
        this(
            config.getString("money.application-name"),
                config.getBoolean("money.enabled"),
                config.getDuration("money.span-timeout", TimeUnit.MILLISECONDS),
                config.getDuration("money.stopped-span-timeout", TimeUnit.MILLISECONDS),
                config.getDuration("money.reaper-interval", TimeUnit.MILLISECONDS),
                new ExecutorSettings(config.getConfig("money.executor")),
                new SchedulerSettings(config.getConfig("money.scheduler"))
        );
    }

    public MoneySettings(String appName, boolean enabled, long spanTimeout, long stoppedSpanTimeout, long reaperInterval, ExecutorSettings executorSettings, SchedulerSettings schedulerSettings) {
        this.appName = appName;
        this.enabled = enabled;
        this.spanTimeout = spanTimeout;
        this.stoppedSpanTimeout = stoppedSpanTimeout;
        this.reaperInterval = reaperInterval;
        this.executorSettings = executorSettings;
        this.schedulerSettings = schedulerSettings;
    }

    public String getAppName() {
        return appName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getSpanTimeout() {
        return spanTimeout;
    }

    public long getStoppedSpanTimeout() {
        return stoppedSpanTimeout;
    }

    public long getReaperInterval() {
        return reaperInterval;
    }

    public ExecutorSettings getExecutorSettings() {
        return executorSettings;
    }

    public SchedulerSettings getSchedulerSettings() {
        return schedulerSettings;
    }
}
