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
