package com.comcast.money.japi;

import java.util.concurrent.ExecutorService;

import com.comcast.money.concurrent.TraceFriendlyThreadPoolExecutor$;

/**
 * Java API for working with Trace Friendly Executors
 */
public class TraceFriendlyExecutors {

    /**
     * @return new {@link com.comcast.money.concurrent.TraceFriendlyThreadPoolExecutor} that is a cached thread pool
     */
    public static ExecutorService newCachedThreadPool() {
        return TraceFriendlyThreadPoolExecutor$.MODULE$.newCachedThreadPool();
    }

    /**
     * @param nThreads the number of threads in the thread pool
     * @return new {@link com.comcast.money.concurrent.TraceFriendlyThreadPoolExecutor} that is a fixed thread pool
     */
    public static ExecutorService newFixedThreadPool(int nThreads) {
        return TraceFriendlyThreadPoolExecutor$.MODULE$.newFixedThreadPool(nThreads);
    }
}
