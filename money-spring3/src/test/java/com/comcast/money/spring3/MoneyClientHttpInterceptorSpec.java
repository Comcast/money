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

import com.comcast.money.api.Note;
import com.comcast.money.api.Span;
import com.comcast.money.api.SpanId;
import com.comcast.money.api.SpanInfo;
import com.comcast.money.core.CoreSpanInfo;
import com.comcast.money.core.Formatters;
import com.comcast.money.core.internal.SpanLocal;
import com.comcast.money.core.Formatters.StringHexHelpers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import java.util.HashMap;
import static org.mockito.Mockito.*;

public class MoneyClientHttpInterceptorSpec {

    private final static String expectedTraceId = "a";
    private final static Long expectedParentSpanId = 1L;
    private final static Long expectedSpanId = 2L;
    private final String expectedMoneyHeaderVal = String.format("trace-id=%s;parent-id=%s;span-id=%s", expectedTraceId, expectedParentSpanId, expectedSpanId);
    private HttpRequest httpRequest = mock(HttpRequest.class);
    private ClientHttpRequestExecution clientHttpRequestExecution = mock(ClientHttpRequestExecution.class);
    private Span span = mock(Span.class);
    private MoneyClientHttpRequestInterceptor moneyClientHttpRequestInterceptor = new MoneyClientHttpRequestInterceptor();

    @Before
    public void setUp() {
        SpanInfo testSpanInfo = new CoreSpanInfo(
                new SpanId(expectedTraceId,expectedParentSpanId,expectedSpanId),
                "testName",
                0L,
                0L,
                0L,
                0L,
                0L,
                Boolean.TRUE,
                new HashMap<String, Note<?>>(),
                "testAppName",
                "testHost");
        when(span.info()).thenReturn(testSpanInfo);
        SpanLocal.push(span);
    }

    @After
    public void tearDown() {
        SpanLocal.clear();
    }

    @Test
    public void testMoneyAndB3HeadersAreSet() throws Exception {
        HttpHeaders httpHeaders = new HttpHeaders();
        when(httpRequest.getHeaders()).thenReturn(httpHeaders);
        moneyClientHttpRequestInterceptor.intercept(httpRequest,"abc".getBytes(), clientHttpRequestExecution);
        verify(httpRequest).getHeaders();
        Assert.assertEquals(4,httpHeaders.size());
        Assert.assertEquals(Formatters.StringHexHelpers(expectedTraceId).fromGuid(), httpHeaders.get("X-B3-TraceId").get(0));
        Assert.assertEquals(expectedParentSpanId.toString(), httpHeaders.get("X-B3-ParentSpanId").get(0));
        Assert.assertEquals(expectedSpanId.toString(), httpHeaders.get("X-B3-SpanId").get(0));
        Assert.assertEquals(expectedMoneyHeaderVal, httpHeaders.get("X-MoneyTrace").get(0));
    }
}

