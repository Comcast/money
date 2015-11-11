package com.comcast.money.basic;

public interface Span {

    void begin(Long startTime);

    void end(Long endTime, boolean result);

    void record(Note<?> note);

    void startTimer(String timerKey, Long startTime);

    void stopTimer(String timerKey, Long endTime);
}
