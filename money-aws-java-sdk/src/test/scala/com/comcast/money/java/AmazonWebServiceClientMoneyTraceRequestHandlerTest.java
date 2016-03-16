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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.amazonaws.Request;
import com.amazonaws.Response;
import com.comcast.money.core.SpanId;

import scala.Option;

@RunWith(MockitoJUnitRunner.class)
public class AmazonWebServiceClientMoneyTraceRequestHandlerTest {

    private AmazonWebServiceClientMoneyTraceRequestHandler handler;

    private final String clientName = "clientName";

    @Mock
    private Request<?> request;

    @Mock
    private SpanId spanId;

    @Mock
    private Exception exception;

    private Response<?> response;

    @Before
    public void setUp() {

        response = new Response<String>(null, null);
        handler = new AmazonWebServiceClientMoneyTraceRequestHandler(clientName);
    }

    @Test
    public void testAmazonWebServiceClientMoneyTraceRequestHandler() {

        assertNotNull(handler);
    }

    @Test
    public void testBeforeRequestRequestOfQ_moneyNotEnabled() {

        handler = spy(handler);

        when(handler.isMoneyEnabled()).thenReturn(false);

        handler.beforeRequest(request);

        verify(handler).isMoneyEnabled();
        verify(handler, never()).getCurrentSpan();
        verifyZeroInteractions(request);
    }

    @Test
    public void testBeforeRequestRequestOfQ_moneyEnabledCurrentSpanIdAvailable() {

        handler = spy(handler);

        when(handler.isMoneyEnabled()).thenReturn(true);
        when(handler.getCurrentSpan()).thenReturn(Option.apply(spanId));
        when(spanId.toHttpHeader()).thenReturn("headerValue");

        handler.beforeRequest(request);

        verify(handler).isMoneyEnabled();
        verify(handler).getCurrentSpan();
        verify(spanId).toHttpHeader();
        verify(request).addHeader(AmazonWebServiceClientMoneyTraceRequestHandler.MONEY_TRACE_HEADER, "headerValue");
    }

    @Test
    public void testBeforeRequestRequestOfQ_moneyEnabledCurrentSpanIdNotAvailable() {

        handler = spy(handler);

        when(handler.isMoneyEnabled()).thenReturn(true);
        Option<SpanId> emptySpanId = Option.empty();
        when(handler.getCurrentSpan()).thenReturn(emptySpanId);

        handler.beforeRequest(request);

        verify(handler).isMoneyEnabled();
        verify(handler).getCurrentSpan();
        verify(spanId, never()).toHttpHeader();
        verify(request, never()).addHeader(AmazonWebServiceClientMoneyTraceRequestHandler.MONEY_TRACE_HEADER, "headerValue");
    }

    @Test
    public void testAfterResponseRequestOfQResponseOfQ_moneyNotEnabled() {

        handler = spy(handler);

        when(handler.isMoneyEnabled()).thenReturn(false);

        handler.afterResponse(request, response);

        verify(handler).isMoneyEnabled();
        verify(handler, never()).stopSpan(true);
    }

    @Test
    public void testAfterResponseRequestOfQResponseOfQ_moneyEnabled() {

        handler = spy(handler);

        when(handler.isMoneyEnabled()).thenReturn(true);

        handler.afterResponse(request, response);

        verify(handler).isMoneyEnabled();
        verify(handler).stopSpan(true);
    }

    @Test
    public void testAfterErrorRequestOfQResponseOfQException_moneyNotEnabled() {

        handler = spy(handler);

        when(handler.isMoneyEnabled()).thenReturn(false);

        handler.afterError(request, response, exception);

        verify(handler).isMoneyEnabled();
        verify(handler, never()).stopSpan(false);
    }

    @Test
    public void testAfterErrorRequestOfQResponseOfQException_moneyEnabled() {

        handler = spy(handler);

        when(handler.isMoneyEnabled()).thenReturn(true);

        handler.afterError(request, response, exception);

        verify(handler).isMoneyEnabled();
        verify(handler).stopSpan(false);
    }

}
