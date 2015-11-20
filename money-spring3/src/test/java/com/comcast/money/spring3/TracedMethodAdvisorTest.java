package com.comcast.money.spring3;

import java.lang.reflect.Method;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.aop.support.StaticMethodMatcherPointcut;

import com.comcast.money.annotations.Traced;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class TracedMethodAdvisorTest {

    @Mock
    private TracedMethodInterceptor tracedMethodInterceptor;

    @Test
    public void testEmptyConstructor() {

        TracedMethodAdvisor underTest = new TracedMethodAdvisor();
        assertThat(underTest.getAdvice()).isNotNull();
        assertThat(underTest.getPointcut()).isNotNull();
    }

    @Test
    public void testConstructor() {
        TracedMethodAdvisor underTest = new TracedMethodAdvisor(tracedMethodInterceptor);
        assertThat(underTest.getAdvice()).isSameAs(tracedMethodInterceptor);
        assertThat(underTest.getPointcut()).isNotNull();
    }

    @Test
    public void testPointcutMatchesMethodWithTracedAnnotation() throws Exception {
        TracedMethodAdvisor underTest = new TracedMethodAdvisor();
        StaticMethodMatcherPointcut pointcut = (StaticMethodMatcherPointcut)underTest.getPointcut();

        Method method = this.getClass().getMethod("doSomething");
        assertThat(pointcut.matches(method, this.getClass())).isTrue();
    }

    @Test
    public void testPointcutDoesNotMatchMethodWithTracedAnnotation() throws Exception {
        TracedMethodAdvisor underTest = new TracedMethodAdvisor();
        StaticMethodMatcherPointcut pointcut = (StaticMethodMatcherPointcut)underTest.getPointcut();

        Method method = this.getClass().getMethod("notTraced");
        assertThat(pointcut.matches(method, this.getClass())).isFalse();
    }

    @Traced("foo")
    public void doSomething() {

    }

    public void notTraced() {

    }
}
