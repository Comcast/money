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

package com.comcast.money.java;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.amazonaws.Request;
import com.amazonaws.Response;
import com.comcast.money.core.SpanId;
import com.comcast.money.internal.SpanLocal;
import com.comcast.money.japi.JMoney;

import scala.Option;

@RunWith(PowerMockRunner.class)
@PrepareForTest({JMoney.class, SpanLocal.class, Request.class, Response.class})
public class AmazonWebServiceClientMoneyTraceRequestHandlerTest {

    private AmazonWebServiceClientMoneyTraceRequestHandler handler;

    private final String clientName = "clientName";

    @Before
    public void setUp() {

        handler = new AmazonWebServiceClientMoneyTraceRequestHandler(clientName);
    }

    @Test
    public void testAmazonWebServiceClientMoneyTraceRequestHandler() {

        assertNotNull(handler);
    }

    @Test
    public void testBeforeRequestRequestOfQ_moneyNotEnabled() {

        PowerMockito.mockStatic(JMoney.class, SpanLocal.class);
        Request<?> request = PowerMockito.mock(Request.class);

        PowerMockito.when(JMoney.isEnabled()).thenReturn(false);

        handler.beforeRequest(request);

        PowerMockito.verifyStatic(Mockito.times(1));
        JMoney.isEnabled();

        PowerMockito.verifyZeroInteractions(SpanLocal.class, request);
    }

    @Test
    public void testBeforeRequestRequestOfQ_moneyEnabledCurrentSpanIdAvailable() {

        PowerMockito.mockStatic(JMoney.class, SpanLocal.class);
        Request<?> request = PowerMockito.mock(Request.class);
        SpanId spanId = PowerMockito.mock(SpanId.class);

        PowerMockito.when(JMoney.isEnabled()).thenReturn(true);
        PowerMockito.when(SpanLocal.current()).thenReturn(Option.apply(spanId));
        PowerMockito.when(spanId.toHttpHeader()).thenReturn("headerValue");

        handler.beforeRequest(request);

        PowerMockito.verifyStatic(Mockito.times(1));
        JMoney.isEnabled();

        PowerMockito.verifyStatic(Mockito.times(1));
        JMoney.startSpan(clientName);

        PowerMockito.verifyStatic(Mockito.times(1));
        SpanLocal.current();

        Mockito.verify(spanId).toHttpHeader();
        Mockito.verify(request).addHeader(AmazonWebServiceClientMoneyTraceRequestHandler.MONEY_TRACE_HEADER, "headerValue");;
    }

    @Test
    public void testBeforeRequestRequestOfQ_moneyEnabledCurrentSpanIdNotAvailable() {

        PowerMockito.mockStatic(JMoney.class, SpanLocal.class);
        Request<?> request = PowerMockito.mock(Request.class);
        SpanId spanId = PowerMockito.mock(SpanId.class);

        PowerMockito.when(JMoney.isEnabled()).thenReturn(true);
        Option<SpanId> emptySpan = Option.empty();
        PowerMockito.when(SpanLocal.current()).thenReturn(emptySpan);

        handler.beforeRequest(request);

        PowerMockito.verifyStatic(Mockito.times(1));
        JMoney.isEnabled();

        PowerMockito.verifyStatic(Mockito.times(1));
        JMoney.startSpan(clientName);

        PowerMockito.verifyStatic(Mockito.times(1));
        SpanLocal.current();

        Mockito.verifyZeroInteractions(spanId, request);
    }

    @Test
    public void testAfterResponseRequestOfQResponseOfQ_moneyNotEnabled() {

        PowerMockito.mockStatic(JMoney.class, SpanLocal.class);
        Request<?> request = PowerMockito.mock(Request.class);
        Response<?> response = PowerMockito.mock(Response.class);

        PowerMockito.when(JMoney.isEnabled()).thenReturn(false);

        handler.beforeRequest(request);

        PowerMockito.verifyStatic(Mockito.times(1));
        JMoney.isEnabled();

        PowerMockito.verifyNoMoreInteractions(JMoney.class);
        PowerMockito.verifyZeroInteractions(request, response);

    }

    @Test
    public void testAfterResponseRequestOfQResponseOfQ_moneyEnabled() {

        PowerMockito.mockStatic(JMoney.class, SpanLocal.class);
        Request<?> request = PowerMockito.mock(Request.class);
        Response<?> response = PowerMockito.mock(Response.class);

        PowerMockito.when(JMoney.isEnabled()).thenReturn(true);

        handler.afterResponse(request, response);

        PowerMockito.verifyStatic(Mockito.times(1));
        JMoney.isEnabled();

        PowerMockito.verifyStatic(Mockito.times(1));
        JMoney.stopSpan(true);

        PowerMockito.verifyZeroInteractions(request, response);

    }

    @Test
    public void testAfterErrorRequestOfQResponseOfQException_moneyNotEnabled() {

        PowerMockito.mockStatic(JMoney.class, SpanLocal.class);
        Request<?> request = PowerMockito.mock(Request.class);
        Response<?> response = PowerMockito.mock(Response.class);
        Exception exception = PowerMockito.mock(Exception.class);

        PowerMockito.when(JMoney.isEnabled()).thenReturn(false);

        handler.afterError(request, response, exception);

        PowerMockito.verifyStatic(Mockito.times(1));
        JMoney.isEnabled();

        PowerMockito.verifyNoMoreInteractions(JMoney.class);
        PowerMockito.verifyZeroInteractions(request, response);
    }

    @Test
    public void testAfterErrorRequestOfQResponseOfQException_moneyEnabled() {

        PowerMockito.mockStatic(JMoney.class, SpanLocal.class);
        Request<?> request = PowerMockito.mock(Request.class);
        Response<?> response = PowerMockito.mock(Response.class);
        Exception exception = PowerMockito.mock(Exception.class);

        PowerMockito.when(JMoney.isEnabled()).thenReturn(true);

        handler.afterError(request, response, exception);

        PowerMockito.verifyStatic(Mockito.times(1));
        JMoney.isEnabled();

        PowerMockito.verifyStatic(Mockito.times(1));
        JMoney.stopSpan(false);

        PowerMockito.verifyZeroInteractions(request, response);
    }

}
