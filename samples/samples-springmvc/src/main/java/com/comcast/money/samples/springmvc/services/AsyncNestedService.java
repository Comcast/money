package com.comcast.money.samples.springmvc.services;

import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

@Component
public class AsyncNestedService {

    @Autowired
    private NestedService nestedService;

    @Async("nestedServiceExecutor")
    public Future<String> doSomethingElse(String message) throws Exception {
        return new AsyncResult<String>(nestedService.doSomethingElse(message));
    }
}
