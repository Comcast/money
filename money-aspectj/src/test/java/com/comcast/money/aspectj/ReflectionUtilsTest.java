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

import java.lang.reflect.Method;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.comcast.money.annotations.Traced;
import com.comcast.money.annotations.TracedData;

import static org.assertj.core.api.Assertions.*;

@RunWith(MockitoJUnitRunner.class)
public class ReflectionUtilsTest {

    private ReflectionUtils underTest = new ReflectionUtils();

    @Test
    public void testNoTracedArguments() throws Exception {
        Method m = this.getClass().getMethod("methodWithNoArguments");

        List<TracedDataParameter> params = underTest.extractTracedParameters(m, new Object[0]);
        assertThat(params).isEmpty();
    }

    @Test
    public void testTracedDataString() throws Exception {
        Method m = this.getClass().getMethod("methodString", String.class);

        List<TracedDataParameter> params = underTest.extractTracedParameters(m, args("foo"));
        TracedDataParameter param = params.get(0);

        assertThat(param.getName()).isEqualTo("traced-param");
        assertThat(param.getParameterType()).isEqualTo(String.class);
        assertThat(param.getParameterValue()).isEqualTo("foo");
    }

    @Test
    public void testTracedDataStringNull() throws Exception {
        Method m = this.getClass().getMethod("methodString", String.class);

        List<TracedDataParameter> params = underTest.extractTracedParameters(m, new Object[1]);
        TracedDataParameter param = params.get(0);

        assertThat(param.getName()).isEqualTo("traced-param");
        assertThat(param.getParameterType()).isEqualTo(String.class);
        assertThat(param.getParameterValue()).isEqualTo(null);
    }

    @Test
    public void testTracedDataMultiple() throws Exception {
        Method m = this.getClass().getMethod("methodMultiple", Long.class, Boolean.class, Double.class, String.class);

        List<TracedDataParameter> params = underTest.extractTracedParameters(m, args(2L, true, 1.3d, "bar"));
        TracedDataParameter param = params.get(0);
        assertThat(param.getName()).isEqualTo("traced-param");
        assertThat(param.getParameterType()).isEqualTo(Long.class);
        assertThat(param.getParameterValue()).isEqualTo(2L);

        param = params.get(1);
        assertThat(param.getName()).isEqualTo("traced-param");
        assertThat(param.getParameterType()).isEqualTo(Boolean.class);
        assertThat(param.getParameterValue()).isEqualTo(true);

        param = params.get(2);
        assertThat(param.getName()).isEqualTo("traced-param");
        assertThat(param.getParameterType()).isEqualTo(Double.class);
        assertThat(param.getParameterValue()).isEqualTo(1.3d);

        param = params.get(3);
        assertThat(param.getName()).isEqualTo("traced-param");
        assertThat(param.getParameterType()).isEqualTo(String.class);
        assertThat(param.getParameterValue()).isEqualTo("bar");
    }

    @Test
    public void testTracedDataIntermingled() throws Exception {
        Method m = this.getClass().getMethod("methodIntermingled", Long.class, Boolean.class, Double.class, String.class);

        List<TracedDataParameter> params = underTest.extractTracedParameters(m, args(2L, true, 1.3d, "bar"));

        assertThat(params.size()).isEqualTo(2);
        TracedDataParameter param = params.get(0);
        assertThat(param.getName()).isEqualTo("traced-param");
        assertThat(param.getParameterType()).isEqualTo(Boolean.class);
        assertThat(param.getParameterValue()).isEqualTo(true);

        param = params.get(1);
        assertThat(param.getName()).isEqualTo("traced-param");
        assertThat(param.getParameterType()).isEqualTo(String.class);
        assertThat(param.getParameterValue()).isEqualTo("bar");
    }

    @Traced("traced")
    public String methodIntermingled(
            Long lng,
            @TracedData("traced-param")Boolean bool,
            Double dbl,
            @TracedData("traced-param")String str) {
        return "hey";
    }

    @Traced("traced")
    public String methodMultiple(
            @TracedData("traced-param")Long lng,
            @TracedData("traced-param")Boolean bool,
            @TracedData("traced-param")Double dbl,
            @TracedData("traced-param")String str) {
        return "hey";
    }

    @Traced("traced")
    public String methodString(@TracedData("traced-param")String param) {
        return "hey";
    }

    @Traced("traced")
    public String methodWithNoArguments() {
        return "hey";
    }

    private Object[] args(Object... arg) {
        return arg;
    }
}
