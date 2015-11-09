package com.comcast.money.samples.springmvc.services;

public class RandomUtil {

    public static double nextRandom(int min, int max) {
        return min + (int)(Math.random() * ((max - min) + 1));
    }
}
