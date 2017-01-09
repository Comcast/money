package com.comcast.money.samples.springmvc.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.comcast.money.core.Money;
import com.comcast.money.annotations.Timed;
import com.comcast.money.annotations.Traced;
import com.comcast.money.annotations.TracedData;

@Component
public class RootService {

    @Autowired
    private AsyncNestedService nestedService;

    @Traced("ROOT_SERVICE")
    public String doSomething(@TracedData("ROOT_NAME")String name) throws Exception {

        double wait = RandomUtil.nextRandom(100, 500);
        Thread.sleep((int)wait);
        String message = name + ", made you wait " + wait + " millseconds.";
        Money.Environment().tracer().record("wait", wait);

        timeIntensiveTask();

        return nestedService.doSomethingElse(message).get();
    }

    @Timed("ROOT_TIMED_TASK")
    private void timeIntensiveTask() throws Exception {
        Thread.sleep(50);
    }
}
