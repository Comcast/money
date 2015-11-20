package com.comcast.money.spring3;

import java.lang.reflect.Method;

import org.aopalliance.aop.Advice;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.comcast.money.annotations.Traced;

@Component
public class TracedMethodAdvisor extends AbstractPointcutAdvisor {

    @Autowired
    private final TracedMethodInterceptor interceptor;

    public TracedMethodAdvisor(TracedMethodInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    public TracedMethodAdvisor() {
        this.interceptor = new TracedMethodInterceptor();
    }

    private static final StaticMethodMatcherPointcut pointcut = new StaticMethodMatcherPointcut() {
        @Override
        public boolean matches(Method method, Class<?> targetClass) {
            return method.isAnnotationPresent(Traced.class);
        }
    };

    @Override
    public Pointcut getPointcut() {
        return pointcut;
    }

    @Override
    public Advice getAdvice() {
        return interceptor;
    }
}
