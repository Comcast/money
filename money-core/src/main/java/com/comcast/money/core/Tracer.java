package com.comcast.money.core;

public interface Tracer {

    void startSpan(String spanName);

    void startSpan(String spanName, boolean propagate);

    void stopSpan(boolean result);

    void stopSpan();

    void record(String key, String value);

    void record(String key, Boolean value);

    void record(String key, Double value);

    void record(String key, Long value);

    void startTimer(String timerKey);

    void stopTimer(String timerKey);
}
