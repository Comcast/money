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

    private ReflectionUtils reflectionUtils;

    public TracedMethodInterceptor() {
        tracer = new SpringTracer();
        reflectionUtils = new ReflectionUtils();
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
