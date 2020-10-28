/*
 * Copyright 2012 Comcast Cable Communications Management, LLC
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

package com.comcast.money.otel.handlers;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.api.trace.SpanContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.comcast.money.api.SpanId;
import com.comcast.money.api.SpanInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        BatchSpanProcessor.class,
        BatchSpanProcessor.Builder.class,
        SimpleSpanProcessor.class,
        SimpleSpanProcessor.Builder.class
})
public class OtelSpanHandlerSpec {

    private SpanExporter spanExporter;
    private BatchSpanProcessor batchSpanProcessor;
    private BatchSpanProcessor.Builder batchSpanProcessorBuilder;
    private SimpleSpanProcessor simpleSpanProcessor;
    private SimpleSpanProcessor.Builder simpleSpanProcessorBuilder;

    private OtelSpanHandler underTest;

    @Before
    public void beforeEach() throws Exception {
        PowerMockito.mockStatic(BatchSpanProcessor.class);
        PowerMockito.mockStatic(SimpleSpanProcessor.class);

        spanExporter = PowerMockito.mock(SpanExporter.class);
        batchSpanProcessor = PowerMockito.mock(BatchSpanProcessor.class);
        batchSpanProcessorBuilder = PowerMockito.mock(BatchSpanProcessor.Builder.class);
        simpleSpanProcessor = PowerMockito.mock(SimpleSpanProcessor.class);
        simpleSpanProcessorBuilder = PowerMockito.mock(SimpleSpanProcessor.Builder.class);

        PowerMockito.when(BatchSpanProcessor.builder(spanExporter)).thenReturn(batchSpanProcessorBuilder);
        PowerMockito.when(SimpleSpanProcessor.builder(spanExporter)).thenReturn(simpleSpanProcessorBuilder);
        PowerMockito.when(batchSpanProcessorBuilder.build()).thenReturn(batchSpanProcessor);
        PowerMockito.when(simpleSpanProcessorBuilder.build()).thenReturn(simpleSpanProcessor);

        underTest = new TestOtelSpanHandler(spanExporter);
    }

    @Test
    public void doesNothingUntilConfigured() {
        SpanInfo spanInfo = PowerMockito.mock(SpanInfo.class);

        underTest.handle(spanInfo);

        PowerMockito.verifyZeroInteractions(spanExporter);
        PowerMockito.verifyStatic(BatchSpanProcessor.class, never());
        BatchSpanProcessor.builder(spanExporter);
        PowerMockito.verifyStatic(SimpleSpanProcessor.class, never());
        SimpleSpanProcessor.builder(spanExporter);
    }

    @Test
    public void configuresSimpleSpanProcessor() {

        Config config = ConfigFactory.parseString(
                "batch = false\n" +
                "export-only-sampled = true"
        );

        underTest.configure(config);

        PowerMockito.verifyStatic(SimpleSpanProcessor.class);
        SimpleSpanProcessor.builder(spanExporter);
        Mockito.verify(simpleSpanProcessorBuilder).setExportOnlySampled(true);
        Mockito.verify(simpleSpanProcessorBuilder).build();

        PowerMockito.verifyStatic(BatchSpanProcessor.class, never());
        BatchSpanProcessor.builder(spanExporter);

        SpanId spanId = SpanId.createNew();
        SpanInfo spanInfo = new TestSpanInfo(spanId);

        underTest.handle(spanInfo);

        ArgumentCaptor<ReadableSpan> captor = ArgumentCaptor.forClass(ReadableSpan.class);
        Mockito.verify(simpleSpanProcessor).onEnd(captor.capture());
        ReadableSpan span = captor.getValue();

        SpanContext spanContext = span.getSpanContext();
        assertThat(spanContext.getTraceIdAsHexString()).isEqualTo(spanId.traceIdAsHex());
        assertThat(spanContext.getSpanIdAsHexString()).isEqualTo(spanId.selfIdAsHex());
    }

    @Test
    public void configuresBatchSpanProcessor() {
        Config config = ConfigFactory.parseString(
                "batch = true\n" +
                "export-only-sampled = true\n" +
                "exporter-timeout-ms = 250\n" +
                "max-batch-size = 500\n" +
                "max-queue-size = 5000\n" +
                "schedule-delay-ms = 1000"
        );

        underTest.configure(config);

        PowerMockito.verifyStatic(BatchSpanProcessor.class);
        BatchSpanProcessor.builder(spanExporter);
        Mockito.verify(batchSpanProcessorBuilder).setExportOnlySampled(true);
        Mockito.verify(batchSpanProcessorBuilder).setExporterTimeoutMillis(250);
        Mockito.verify(batchSpanProcessorBuilder).setMaxExportBatchSize(500);
        Mockito.verify(batchSpanProcessorBuilder).setMaxQueueSize(5000);
        Mockito.verify(batchSpanProcessorBuilder).setScheduleDelayMillis(1000);
        Mockito.verify(batchSpanProcessorBuilder).build();

        PowerMockito.verifyStatic(SimpleSpanProcessor.class, never());
        SimpleSpanProcessor.builder(spanExporter);

        SpanId spanId = SpanId.createNew();
        SpanInfo spanInfo = new TestSpanInfo(spanId);

        underTest.handle(spanInfo);

        ArgumentCaptor<ReadableSpan> captor = ArgumentCaptor.forClass(ReadableSpan.class);
        Mockito.verify(batchSpanProcessor).onEnd(captor.capture());
        ReadableSpan span = captor.getValue();

        SpanContext spanContext = span.getSpanContext();
        assertThat(spanContext.getTraceIdAsHexString()).isEqualTo(spanId.traceIdAsHex());
        assertThat(spanContext.getSpanIdAsHexString()).isEqualTo(spanId.selfIdAsHex());
    }

    static class TestOtelSpanHandler extends OtelSpanHandler {
        private final SpanExporter spanExporter;

        public TestOtelSpanHandler(SpanExporter spanExporter) {
            this.spanExporter = spanExporter;
        }

        @Override
        public SpanExporter createSpanExporter(Config config) {
            return spanExporter;
        }
    }
}
