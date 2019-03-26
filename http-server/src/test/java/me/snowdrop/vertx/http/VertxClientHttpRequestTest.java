package me.snowdrop.vertx.http;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import io.netty.buffer.ByteBufAllocator;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class VertxClientHttpRequestTest {

    @Mock
    private HttpClientRequest mockHttpClientRequest;

    private NettyDataBufferFactory nettyDataBufferFactory;

    private VertxClientHttpRequest request;

    @Before
    public void setUp() {
        nettyDataBufferFactory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
        request = new VertxClientHttpRequest(mockHttpClientRequest, nettyDataBufferFactory);
    }

    @Test
    public void shouldGetMethod() {
        given(mockHttpClientRequest.method()).willReturn(io.vertx.core.http.HttpMethod.GET);

        HttpMethod method = request.getMethod();

        assertThat(method).isEqualTo(HttpMethod.GET);
    }

    @Test
    public void shouldGetUri() {
        given(mockHttpClientRequest.absoluteURI()).willReturn("http://example.com");

        URI expectedUri = URI.create("http://example.com");
        URI actualUri = request.getURI();

        assertThat(actualUri).isEqualTo(expectedUri);
    }

    @Test
    public void shouldGetBufferFactory() {
        DataBufferFactory dataBufferFactory = request.bufferFactory();

        assertThat(dataBufferFactory).isEqualTo(nettyDataBufferFactory);
    }

    @Test
    public void shouldWriteFromPublisher() {
        Buffer firstChunk = Buffer.buffer("chunk 1");
        Buffer secondChunk = Buffer.buffer("chunk 2");

        TestPublisher<DataBuffer> source = TestPublisher.create();
        Mono<Void> result = request.writeWith(source);

        StepVerifier.create(result)
            .expectSubscription()
            .then(() -> source.assertMinRequested(1))
            .then(() -> source.next(nettyDataBufferFactory.wrap(firstChunk.getByteBuf())))
            .then(() -> source.assertMinRequested(1))
            .then(() -> source.next(nettyDataBufferFactory.wrap(secondChunk.getByteBuf())))
            .then(() -> source.assertMinRequested(1))
            .then(source::complete)
            .verifyComplete();

        verify(mockHttpClientRequest).write(firstChunk);
        verify(mockHttpClientRequest).write(secondChunk);
        verify(mockHttpClientRequest).end();
    }

    @Test
    public void shouldWriteFromPublisherAndFlush() {
        Buffer firstChunk = Buffer.buffer("chunk 1");
        Buffer secondChunk = Buffer.buffer("chunk 2");

        TestPublisher<DataBuffer> source = TestPublisher.create();
        Mono<Void> result = request.writeAndFlushWith(Flux.just(source));

        StepVerifier.create(result)
            .expectSubscription()
            .then(() -> source.assertMinRequested(1))
            .then(() -> source.next(nettyDataBufferFactory.wrap(firstChunk.getByteBuf())))
            .then(() -> source.assertMinRequested(1))
            .then(() -> source.next(nettyDataBufferFactory.wrap(secondChunk.getByteBuf())))
            .then(() -> source.assertMinRequested(1))
            .then(source::complete)
            .verifyComplete();

        verify(mockHttpClientRequest).write(firstChunk);
        verify(mockHttpClientRequest).write(secondChunk);
        verify(mockHttpClientRequest).end();
    }

    @Test
    public void shouldApplyHeaders() {
        request.getHeaders().put("key1", Arrays.asList("value1", "value2"));
        request.getHeaders().add("key2", "value3");

        request.applyHeaders();

        verify(mockHttpClientRequest).putHeader("key1", (Iterable<String>) Arrays.asList("value1", "value2"));
        verify(mockHttpClientRequest).putHeader("key2", (Iterable<String>) Collections.singletonList("value3"));
    }
}
