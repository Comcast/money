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

package com.comcast.money.otel.handlers.jaeger;

import java.util.Collection;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.opentelemetry.exporters.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
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
import com.comcast.money.otel.handlers.TestSpanInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        JaegerGrpcSpanExporter.class,
        JaegerGrpcSpanExporter.Builder.class
})
public class JaegerOtelSpanHandlerSpec {

    private JaegerGrpcSpanExporter spanExporter;
    private JaegerGrpcSpanExporter.Builder spanExporterBuilder;

    private JaegerOtelSpanHandler underTest;

    @Before
    public void beforeEach() throws Exception {
        PowerMockito.mockStatic(JaegerGrpcSpanExporter.class);

        spanExporter = PowerMockito.mock(JaegerGrpcSpanExporter.class);
        spanExporterBuilder = PowerMockito.mock(JaegerGrpcSpanExporter.Builder.class);

        PowerMockito.when(JaegerGrpcSpanExporter.builder()).thenReturn(spanExporterBuilder);
        PowerMockito.when(spanExporterBuilder.build()).thenReturn(spanExporter);
        PowerMockito.when(spanExporter.export(any())).thenReturn(CompletableResultCode.ofSuccess());

        underTest = new JaegerOtelSpanHandler();
    }

    @Test
    public void configuresJaegerExporter() {
        Config config = ConfigFactory.parseString(
                "batch = false\n" +
                "export-only-sampled = true\n" +
                "exporter {\n" +
                "  service-name = \"service-name\"\n" +
                "}"
        );

        underTest.configure(config);

        PowerMockito.verifyStatic(JaegerGrpcSpanExporter.class);
        JaegerGrpcSpanExporter.builder();
        Mockito.verify(spanExporterBuilder).setServiceName("service-name");
        Mockito.verify(spanExporterBuilder).build();

        SpanId spanId = SpanId.createNew();
        SpanInfo spanInfo = new TestSpanInfo(spanId);

        underTest.handle(spanInfo);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<SpanData>> captor = ArgumentCaptor.forClass(Collection.class);
        Mockito.verify(spanExporter).export(captor.capture());
        Collection<SpanData> collection = captor.getValue();

        assertThat(collection).hasSize(1);
        SpanData spanData = collection.stream().findFirst().get();

        assertThat(spanData.getTraceId()).isEqualTo(spanId.traceIdAsHex());
        assertThat(spanData.getSpanId()).isEqualTo(spanId.selfIdAsHex());
    }

    @Test
    public void configuresJaegerExporterWithEndpoint() {
        Config config = ConfigFactory.parseString(
                "batch = false\n" +
                        "export-only-sampled = true\n" +
                        "exporter {\n" +
                        "  service-name = \"service-name\"\n" +
                        "  endpoint = \"endpoint\"\n" +
                        "}"
        );

        underTest.configure(config);

        PowerMockito.verifyStatic(JaegerGrpcSpanExporter.class);
        JaegerGrpcSpanExporter.builder();
        Mockito.verify(spanExporterBuilder).setServiceName("service-name");
        Mockito.verify(spanExporterBuilder).setEndpoint("endpoint");
        Mockito.verify(spanExporterBuilder, never()).setDeadlineMs(anyLong());
        Mockito.verify(spanExporterBuilder).build();
    }

    @Test
    public void configuresJaegerExporterWithDeadline() {
        Config config = ConfigFactory.parseString(
                "batch = false\n" +
                        "export-only-sampled = true\n" +
                        "exporter {\n" +
                        "  service-name = \"service-name\"\n" +
                        "  deadline-ms = 1000\n" +
                        "}"
        );

        underTest.configure(config);

        PowerMockito.verifyStatic(JaegerGrpcSpanExporter.class);
        JaegerGrpcSpanExporter.builder();
        Mockito.verify(spanExporterBuilder).setServiceName("service-name");
        Mockito.verify(spanExporterBuilder, never()).setEndpoint(anyString());
        Mockito.verify(spanExporterBuilder).setDeadlineMs(1000);
        Mockito.verify(spanExporterBuilder).build();
    }
}
