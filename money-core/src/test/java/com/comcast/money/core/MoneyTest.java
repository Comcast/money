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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MoneyTest {

    private ExecutorService executorService = Executors.newFixedThreadPool(10);
    @Test
    public void testLogging() throws Exception {

        Money.tracer.startSpan("bar");
        Money.tracer.startSpan("childOfBar");

        for (int i = 0; i < 100; i++) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 100; j++) {
                        try {
                            Money.tracer.startSpan("foo");
                            Money.tracer.record("hey", "there");
                            Money.tracer.record("dbl", 0.0);
                            Money.tracer.record("lng", 100L);
                            Money.tracer.record("bool", false);

                            Money.tracer.startSpan("fooChild");
                            Money.tracer.record("child", "child");
                            Thread.sleep(10);
                            Money.tracer.stopSpan();
                        } catch(Exception e) {

                        } finally {
                            Money.tracer.stopSpan(true);
                        }
                    }
                }
            });
        }

        Thread.sleep(10000);
    }

}
