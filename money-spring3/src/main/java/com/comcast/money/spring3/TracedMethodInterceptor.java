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

package com.comcast.money.spring3;

import java.lang.reflect.Method;
import java.util.List;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.comcast.money.annotations.Traced;
import com.comcast.money.reflect.ReflectionUtils;
import com.comcast.money.reflect.TracedDataParameter;

@Component
public class TracedMethodInterceptor implements MethodInterceptor {

    @Autowired
    private SpringTracer tracer;

    private ReflectionUtils reflectionUtils = new ReflectionUtils();

    public TracedMethodInterceptor() {
    }

    public TracedMethodInterceptor(SpringTracer tracer, ReflectionUtils reflectionUtils) {
        this.tracer = tracer;
        this.reflectionUtils = reflectionUtils;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        final Traced annotation = invocation.getStaticPart().getAnnotation(Traced.class);
        boolean success = true;

        if (annotation != null) {
            try {
                tracer.startSpan(annotation.value());

                Method method = invocation.getMethod();
                Object[] arguments = invocation.getArguments();

                List<TracedDataParameter> tracedParams = reflectionUtils.extractTracedParameters(method, arguments);
                for (TracedDataParameter tracedParam : tracedParams) {
                    tracedParam.trace(tracer);
                }

                return invocation.proceed();
            } catch (Throwable t) {
                success = false;
                throw t;
            } finally {
                tracer.stopSpan(success);
            }
        } else {
            return invocation.proceed();
        }
    }
}
