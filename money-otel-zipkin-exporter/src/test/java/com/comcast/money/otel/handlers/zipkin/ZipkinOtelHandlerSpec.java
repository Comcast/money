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

package com.comcast.money.otel.handlers.zipkin;

import java.util.Collection;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
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
import zipkin2.codec.SpanBytesEncoder;

import com.comcast.money.api.SpanId;
import com.comcast.money.api.SpanInfo;
import com.comcast.money.otel.handlers.TestSpanInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        ZipkinSpanExporter.class,
        ZipkinSpanExporter.Builder.class
})
public class ZipkinOtelHandlerSpec {

    private ZipkinSpanExporter spanExporter;
    private ZipkinSpanExporter.Builder spanExporterBuilder;

    private ZipkinOtelSpanHandler underTest;

    @Before
    public void beforeEach() throws Exception {
        PowerMockito.mockStatic(ZipkinSpanExporter.class);

        spanExporter = PowerMockito.mock(ZipkinSpanExporter.class);
        spanExporterBuilder = PowerMockito.mock(ZipkinSpanExporter.Builder.class);

        PowerMockito.when(ZipkinSpanExporter.builder()).thenReturn(spanExporterBuilder);
        PowerMockito.when(spanExporterBuilder.build()).thenReturn(spanExporter);
        PowerMockito.when(spanExporter.export(any())).thenReturn(CompletableResultCode.ofSuccess());

        underTest = new ZipkinOtelSpanHandler();
    }

    @Test
    public void configuresZipkinExporter() {
        Config config = ConfigFactory.parseString(
                "batch = false\n" +
                "export-only-sampled = true\n" +
                "exporter {\n" +
                "  service-name = \"service-name\"\n" +
                "}"
        );

        underTest.configure(config);

        PowerMockito.verifyStatic(ZipkinSpanExporter.class);
        ZipkinSpanExporter.builder();
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
    public void configuresZipkinExporterWithEndpoint() {
        Config config = ConfigFactory.parseString(
                "batch = false\n" +
                        "export-only-sampled = true\n" +
                        "exporter {\n" +
                        "  service-name = \"service-name\"\n" +
                        "  endpoint = \"endpoint\"\n" +
                        "}"
        );

        underTest.configure(config);

        PowerMockito.verifyStatic(ZipkinSpanExporter.class);
        ZipkinSpanExporter.builder();
        Mockito.verify(spanExporterBuilder).setServiceName("service-name");
        Mockito.verify(spanExporterBuilder).setEndpoint("endpoint");
        Mockito.verify(spanExporterBuilder, never()).setEncoder(any());
        Mockito.verify(spanExporterBuilder).build();
    }

    @Test
    public void configuresZipkinExporterWithJsonV1Encoder() {
        Config config = ConfigFactory.parseString(
                "batch = false\n" +
                        "export-only-sampled = true\n" +
                        "exporter {\n" +
                        "  service-name = \"service-name\"\n" +
                        "  encoder = \"json-v1\"\n" +
                        "}"
        );

        underTest.configure(config);

        PowerMockito.verifyStatic(ZipkinSpanExporter.class);
        ZipkinSpanExporter.builder();
        Mockito.verify(spanExporterBuilder).setServiceName("service-name");
        Mockito.verify(spanExporterBuilder, never()).setEndpoint(anyString());
        Mockito.verify(spanExporterBuilder).setEncoder(SpanBytesEncoder.JSON_V1);
        Mockito.verify(spanExporterBuilder).build();
    }

    @Test
    public void configuresZipkinExporterWithJsonV2Encoder() {
        Config config = ConfigFactory.parseString(
                "batch = false\n" +
                        "export-only-sampled = true\n" +
                        "exporter {\n" +
                        "  service-name = \"service-name\"\n" +
                        "  encoder = \"json-v2\"\n" +
                        "}"
        );

        underTest.configure(config);

        PowerMockito.verifyStatic(ZipkinSpanExporter.class);
        ZipkinSpanExporter.builder();
        Mockito.verify(spanExporterBuilder).setServiceName("service-name");
        Mockito.verify(spanExporterBuilder, never()).setEndpoint(anyString());
        Mockito.verify(spanExporterBuilder).setEncoder(SpanBytesEncoder.JSON_V2);
        Mockito.verify(spanExporterBuilder).build();
    }

    @Test
    public void configuresZipkinExporterWithThriftEncoder() {
        Config config = ConfigFactory.parseString(
                "batch = false\n" +
                        "export-only-sampled = true\n" +
                        "exporter {\n" +
                        "  service-name = \"service-name\"\n" +
                        "  encoder = \"thrift\"\n" +
                        "}"
        );

        underTest.configure(config);

        PowerMockito.verifyStatic(ZipkinSpanExporter.class);
        ZipkinSpanExporter.builder();
        Mockito.verify(spanExporterBuilder).setServiceName("service-name");
        Mockito.verify(spanExporterBuilder, never()).setEndpoint(anyString());
        Mockito.verify(spanExporterBuilder).setEncoder(SpanBytesEncoder.THRIFT);
        Mockito.verify(spanExporterBuilder).build();
    }

    @Test
    public void configuresZipkinExporterWithProto3Encoder() {
        Config config = ConfigFactory.parseString(
                "batch = false\n" +
                        "export-only-sampled = true\n" +
                        "exporter {\n" +
                        "  service-name = \"service-name\"\n" +
                        "  encoder = \"proto3\"\n" +
                        "}"
        );

        underTest.configure(config);

        PowerMockito.verifyStatic(ZipkinSpanExporter.class);
        ZipkinSpanExporter.builder();
        Mockito.verify(spanExporterBuilder).setServiceName("service-name");
        Mockito.verify(spanExporterBuilder, never()).setEndpoint(anyString());
        Mockito.verify(spanExporterBuilder).setEncoder(SpanBytesEncoder.PROTO3);
        Mockito.verify(spanExporterBuilder).build();
    }

    @Test
    public void configuresZipkinExporterWithUnknownEncoder() {
        Config config = ConfigFactory.parseString(
                "batch = false\n" +
                        "export-only-sampled = true\n" +
                        "exporter {\n" +
                        "  service-name = \"service-name\"\n" +
                        "  encoder = \"unknown\"\n" +
                        "}"
        );

        assertThatThrownBy(() -> underTest.configure(config))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unknown encoder 'unknown'.");
    }
}
