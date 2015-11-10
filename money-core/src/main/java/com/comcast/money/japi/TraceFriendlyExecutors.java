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
