package com.comcast.money.samples.springmvc.controllers;

import java.lang.IllegalArgumentException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.comcast.money.annotations.Traced;
import com.comcast.money.annotations.TracedData;
import com.comcast.money.samples.springmvc.services.AsyncRootService;


@RestController
@RequestMapping("/hello")
public class SampleController {

    private static final Logger logger = LoggerFactory.getLogger(SampleController.class);

    @Autowired
    private AsyncRootService rootService;

    @Traced(value="SAMPLE_CONTROLLER", ignoredExceptions={IllegalArgumentException.class})
    @RequestMapping(method = RequestMethod.GET, value = "/{name}")
    public String hello(@TracedData(value="CONTROLLER_INPUT", propagate=true) @PathVariable("name") String name) throws Exception {

        logger.warn("Call to sample controller with name " + name);

        if ("ignore".equals(name)) {
            throw new IllegalArgumentException("this should be ignored, check the log file for span-success=true");
        }
        return rootService.doSomething(name).get();
    }
}
