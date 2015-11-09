package com.comcast.money.samples.springmvc.services;

import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

@Component
public class AsyncRootService {

    @Autowired
    private RootService rootService;

    @Async("rootServiceExecutor")
    public Future<String> doSomething(String name) throws Exception {
        return new AsyncResult<String>(rootService.doSomething(name));
    }
}
