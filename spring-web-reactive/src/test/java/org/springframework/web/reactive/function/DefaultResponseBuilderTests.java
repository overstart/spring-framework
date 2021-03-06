/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.reactive.function;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.web.reactive.result.view.View;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.MockWebSessionManager;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Arjen Poutsma
 */
public class DefaultResponseBuilderTests {

	@Test
	public void from() throws Exception {
		Response<Void> other = Response.ok().header("foo", "bar").build();
		Response<Void> result = Response.from(other).build();
		assertEquals(HttpStatus.OK, result.statusCode());
		assertEquals("bar", result.headers().getFirst("foo"));
	}

	@Test
	public void status() throws Exception {
		Response<Void> result = Response.status(HttpStatus.CREATED).build();
		assertEquals(HttpStatus.CREATED, result.statusCode());
	}

	@Test
	public void statusInt() throws Exception {
		Response<Void> result = Response.status(201).build();
		assertEquals(HttpStatus.CREATED, result.statusCode());
	}

	@Test
	public void ok() throws Exception {
		Response<Void> result = Response.ok().build();
		assertEquals(HttpStatus.OK, result.statusCode());
	}

	@Test
	public void created() throws Exception {
		URI location = URI.create("http://example.com");
		Response<Void> result = Response.created(location).build();
		assertEquals(HttpStatus.CREATED, result.statusCode());
		assertEquals(location, result.headers().getLocation());
	}

	@Test
	public void accepted() throws Exception {
		Response<Void> result = Response.accepted().build();
		assertEquals(HttpStatus.ACCEPTED, result.statusCode());
	}

	@Test
	public void noContent() throws Exception {
		Response<Void> result = Response.noContent().build();
		assertEquals(HttpStatus.NO_CONTENT, result.statusCode());
	}

	@Test
	public void badRequest() throws Exception {
		Response<Void> result = Response.badRequest().build();
		assertEquals(HttpStatus.BAD_REQUEST, result.statusCode());
	}

	@Test
	public void notFound() throws Exception {
		Response<Void> result = Response.notFound().build();
		assertEquals(HttpStatus.NOT_FOUND, result.statusCode());
	}

	@Test
	public void unprocessableEntity() throws Exception {
		Response<Void> result = Response.unprocessableEntity().build();
		assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, result.statusCode());
	}

	@Test
	public void allow() throws Exception {
		Response<Void> result = Response.ok().allow(HttpMethod.GET).build();
		assertEquals(Collections.singleton(HttpMethod.GET), result.headers().getAllow());
	}

	@Test
	public void contentLength() throws Exception {
		Response<Void> result = Response.ok().contentLength(42).build();
		assertEquals(42, result.headers().getContentLength());
	}

	@Test
	public void contentType() throws Exception {
		Response<Void> result = Response.ok().contentType(MediaType.APPLICATION_JSON).build();
		assertEquals(MediaType.APPLICATION_JSON, result.headers().getContentType());
	}

	@Test
	public void eTag() throws Exception {
		Response<Void> result = Response.ok().eTag("foo").build();
		assertEquals("\"foo\"", result.headers().getETag());
	}

	@Test
	public void lastModified() throws Exception {
		ZonedDateTime now = ZonedDateTime.now();
		Response<Void> result = Response.ok().lastModified(now).build();
		assertEquals(now.toInstant().toEpochMilli()/1000, result.headers().getLastModified()/1000);
	}

	@Test
	public void cacheControlTag() throws Exception {
		Response<Void> result = Response.ok().cacheControl(CacheControl.noCache()).build();
		assertEquals("no-cache", result.headers().getCacheControl());
	}

	@Test
	public void varyBy() throws Exception {
		Response<Void> result = Response.ok().varyBy("foo").build();
		assertEquals(Collections.singletonList("foo"), result.headers().getVary());
	}

	@Test
	public void statusCode() throws Exception {
		HttpStatus statusCode = HttpStatus.ACCEPTED;
		Response<Void> result = Response.status(statusCode).build();
		assertSame(statusCode, result.statusCode());
	}

	@Test
	public void headers() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		Response<Void> result = Response.ok().headers(headers).build();
		assertEquals(headers, result.headers());
	}

	@Test
	public void build() throws Exception {
		Response<Void> result = Response.status(201).header("MyKey", "MyValue").build();

		ServerWebExchange exchange = mock(ServerWebExchange.class);
		MockServerHttpResponse response = new MockServerHttpResponse();
		when(exchange.getResponse()).thenReturn(response);

		result.writeTo(exchange).block();
		assertEquals(201, response.getStatusCode().value());
		assertEquals("MyValue", response.getHeaders().getFirst("MyKey"));
		assertNull(response.getBody());

	}

	@Test
	public void buildVoidPublisher() throws Exception {
		Mono<Void> mono = Mono.empty();
		Response<Mono<Void>> result = Response.ok().build(mono);

		ServerWebExchange exchange = mock(ServerWebExchange.class);
		MockServerHttpResponse response = new MockServerHttpResponse();
		when(exchange.getResponse()).thenReturn(response);

		result.writeTo(exchange).block();
		assertNull(response.getBody());
	}

	@Test
	public void body() throws Exception {
		String body = "foo";
		Response<String> result = Response.ok().body(body);
		assertEquals(body, result.body());

		MockServerHttpRequest request =
				new MockServerHttpRequest(HttpMethod.GET, "http://localhost");
		MockServerHttpResponse response = new MockServerHttpResponse();
		ServerWebExchange exchange =
				new DefaultServerWebExchange(request, response, new MockWebSessionManager());
		Set<HttpMessageWriter<?>>
				messageWriters = Collections
				.singleton(new EncoderHttpMessageWriter<CharSequence>(new CharSequenceEncoder()));
		exchange.getAttributes().put(Router.HTTP_MESSAGE_WRITERS_ATTRIBUTE,
				(Supplier<Stream<HttpMessageWriter<?>>>) messageWriters::stream);

		result.writeTo(exchange).block();
		assertNotNull(response.getBody());
	}

	@Test
	public void bodyNotAcceptable() throws Exception {
		String body = "foo";
		Response<String> result = Response.ok().contentType(MediaType.APPLICATION_JSON).body(body);
		assertEquals(body, result.body());

		MockServerHttpRequest request =
				new MockServerHttpRequest(HttpMethod.GET, "http://localhost");
		MockServerHttpResponse response = new MockServerHttpResponse();
		ServerWebExchange exchange =
				new DefaultServerWebExchange(request, response, new MockWebSessionManager());
		Set<HttpMessageWriter<?>>
				messageWriters = Collections
				.singleton(new EncoderHttpMessageWriter<CharSequence>(new CharSequenceEncoder()));
		exchange.getAttributes().put(Router.HTTP_MESSAGE_WRITERS_ATTRIBUTE,
				(Supplier<Stream<HttpMessageWriter<?>>>) messageWriters::stream);

		result.writeTo(exchange).block();
		assertEquals(HttpStatus.NOT_ACCEPTABLE, response.getStatusCode());
	}

	@Test
	public void stream() throws Exception {
		Publisher<String> publisher = Flux.just("foo", "bar");
		Response<Publisher<String>> result = Response.ok().stream(publisher, String.class);

		MockServerHttpRequest request =
				new MockServerHttpRequest(HttpMethod.GET, "http://localhost");
		MockServerHttpResponse response = new MockServerHttpResponse();
		ServerWebExchange exchange =
				new DefaultServerWebExchange(request, response, new MockWebSessionManager());
		Set<HttpMessageWriter<?>> messageWriters = Collections
				.singleton(new EncoderHttpMessageWriter<CharSequence>(new CharSequenceEncoder()));
		exchange.getAttributes().put(Router.HTTP_MESSAGE_WRITERS_ATTRIBUTE,
				(Supplier<Stream<HttpMessageWriter<?>>>) messageWriters::stream);

		result.writeTo(exchange).block();
		assertNotNull(response.getBody());
	}

	@Test
	public void resource() throws Exception {
		Resource resource = new ClassPathResource("response.txt", DefaultResponseBuilderTests.class);
		Response<Resource> result = Response.ok().resource(resource);

		ServerWebExchange exchange = mock(ServerWebExchange.class);
		MockServerHttpResponse response = new MockServerHttpResponse();
		when(exchange.getResponse()).thenReturn(response);


		result.writeTo(exchange).block();
		assertNotNull(response.getBody());
	}

	@Test
	public void sse() throws Exception {
		ServerSentEvent<String> sse = ServerSentEvent.<String>builder().data("42").build();
		Mono<ServerSentEvent<String>> body = Mono.just(sse);
		Response<Mono<ServerSentEvent<String>>> result = Response.ok().sse(body);

		ServerWebExchange exchange = mock(ServerWebExchange.class);
		MockServerHttpResponse response = new MockServerHttpResponse();
		when(exchange.getResponse()).thenReturn(response);

		result.writeTo(exchange).block();
		assertNotNull(response.getBodyWithFlush());
	}

	@Test
	public void render() throws Exception {
		Map<String, Object> model = Collections.singletonMap("foo", "bar");
		Response<Rendering> result = Response.ok().render("view", model);

		assertEquals("view", result.body().name());
		assertEquals(model, result.body().model());

		MockServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, URI.create("http://localhost"));
		MockServerHttpResponse response = new MockServerHttpResponse();
		ServerWebExchange exchange = new DefaultServerWebExchange(request, response, new MockWebSessionManager());
		ViewResolver viewResolver = mock(ViewResolver.class);
		View view = mock(View.class);
		when(viewResolver.resolveViewName("view", Locale.ENGLISH)).thenReturn(Mono.just(view));
		when(view.render(model, null, exchange)).thenReturn(Mono.empty());
		exchange.getAttributes().put(Router.VIEW_RESOLVERS_ATTRIBUTE,
				(Supplier<Stream<ViewResolver>>) () -> Collections
						.singleton(viewResolver).stream());


		result.writeTo(exchange).block();
	}

	@Test
	public void renderObjectArray() throws Exception {
		Response<Rendering> result =
				Response.ok().render("name", this, Collections.emptyList(), "foo");
		Map<String, Object> model = result.body().model();
		assertEquals(2, model.size());
		assertEquals(this, model.get("defaultResponseBuilderTests"));
		assertEquals("foo", model.get("string"));
	}

}