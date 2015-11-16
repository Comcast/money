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
