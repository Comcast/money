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

package com.comcast.money.aspectj;

import java.util.List;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import com.comcast.money.annotations.Traced;
import com.comcast.money.core.Money;
import com.comcast.money.core.Tracer;
import com.comcast.money.reflect.ReflectionUtils;
import com.comcast.money.reflect.TracedDataParameter;

@Aspect
public class TraceAspect {

    private Tracer tracer = Money.tracer;
    private ReflectionUtils reflectionUtils = new ReflectionUtils();

    @Pointcut("execution(@com.comcast.money.annotations.Traced * *(..)) && @annotation(traceAnnotation)")
    public void traced(Traced traceAnnotation) {

    }

    @Around("traced(traceAnnotation)")
    public Object adviseMethodsWithTracing(ProceedingJoinPoint joinPoint, Traced traceAnnotation) throws Throwable {
        System.out.println("\r\n!!!Tracing Aspect!!!");
        // TODO: MDC SUPPORT
        String key = traceAnnotation.value();
        Boolean result = true;
        try {
            tracer.startSpan(key);
            traceMethodArguments(joinPoint);
            return joinPoint.proceed();
        } catch (Throwable t) {
            result = false;
            throw t;
        } finally {
            tracer.stopSpan(result);
        }
    }

    private void traceMethodArguments(JoinPoint joinPoint) {
        if (joinPoint.getArgs() != null) {
            Signature signature = joinPoint.getStaticPart().getSignature();
            if (signature instanceof MethodSignature) {
                MethodSignature methodSignature = (MethodSignature) signature;
                List<TracedDataParameter> tracedParams = reflectionUtils.extractTracedParameters(methodSignature.getMethod(), joinPoint.getArgs());
                for (TracedDataParameter tracedParam : tracedParams) {
                    tracedParam.trace(tracer);
                }
            }
        }
    }
}
