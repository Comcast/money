package com.comcast.money.basic;

public class TimeUtils {

    /**
     * @return The current nano time in microseconds
     */
    public static Long thisInstant() {
        return System.nanoTime() / 1000;
    }
}
