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
