/*
 *  iCure Data Stack. Copyright (c) 2020  aduchate
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful, but
 *     WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public
 *     License along with this program.  If not, see
 *     <https://www.gnu.org/licenses/>.
 */

package org.taktik.couchdb.springframework.webclient

import io.icure.asyncjacksonhttpclient.exception.TimeoutException
import io.icure.asyncjacksonhttpclient.net.web.HttpMethod
import io.icure.asyncjacksonhttpclient.net.web.Request
import io.icure.asyncjacksonhttpclient.net.web.Response
import io.icure.asyncjacksonhttpclient.net.web.ResponseStatus
import io.icure.asyncjacksonhttpclient.net.web.WebClient
import io.netty.handler.codec.http.DefaultHttpHeaders
import io.netty.handler.codec.http.HttpHeaders
import io.netty.handler.timeout.ReadTimeoutException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactor.asFlux
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClientRequestException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClientRequest
import java.net.URI
import java.nio.ByteBuffer
import java.time.Duration
import java.util.AbstractMap
import java.util.function.Consumer

@OptIn(ExperimentalCoroutinesApi::class)
class SpringWebfluxWebClient(val reactorClientHttpConnector: ReactorClientHttpConnector? = null, val filters: Consumer<MutableList<ExchangeFilterFunction>>? = null) : WebClient {
	override fun uri(uri: URI): Request {
		return SpringWebfluxRequest(
			org.springframework.web.reactive.function.client.WebClient.builder()
				.let { c -> reactorClientHttpConnector?.let { c.clientConnector(it) } ?: c }
				.let { c -> filters?.let { c.filters(it) } ?: c }
				.build(),
			uri,
			null,
			DefaultHttpHeaders(),
			null,
			null
		)
	}
}

@OptIn(ExperimentalCoroutinesApi::class)
class SpringWebfluxRequest(
	private val client: org.springframework.web.reactive.function.client.WebClient,
	private val uri: URI,
	private val method: HttpMethod?,
	private val headers: HttpHeaders,
	private val bodyPublisher: Flow<ByteBuffer>?,
	private val requestTimeout: Duration?,
) : Request {
	override fun method(method: HttpMethod, timeoutDuration: Duration?): Request = SpringWebfluxRequest(client, uri, method, headers, bodyPublisher, timeoutDuration)
	override fun header(name: String, value: String): Request = SpringWebfluxRequest(client, uri, method, headers.add(name, value), bodyPublisher, requestTimeout)
	override fun body(producer: Flow<ByteBuffer>): Request = SpringWebfluxRequest(client, uri, method, headers, producer, requestTimeout)
	override fun retrieve() = SpringWebfluxResponse(
		headers.entries().fold(client.method(method.toSpringMethod()).uri(uri).let { req ->
			if (requestTimeout != null) {
				req.httpRequest {
					it.getNativeRequest<HttpClientRequest>().responseTimeout(requestTimeout)
				}
			} else req
		}) { acc, (name, value) -> acc.header(name, value) }.let {
			bodyPublisher?.let { bp -> it.body(bp.asFlux(), ByteBuffer::class.java) } ?: it
		},
		emptyMap(),
		emptyMap(),
		null,
		requestTimeout
	)

	override fun toString(): String {
		return "-X $method $uri"
	}
}

@OptIn(ExperimentalCoroutinesApi::class)
class SpringWebfluxResponse(
	private val requestHeaderSpec: org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec<*>,
	private val statusHandlers: Map<Int, (ResponseStatus) -> Mono<out Throwable>>,
	private val headerHandler: Map<String, (String) -> Mono<Unit>>,
	private val timingHandler: ((Long) -> Mono<Unit>)?,
	private val requestTimeout: Duration?,
) : Response {
	override fun onStatus(status: Int, handler: (ResponseStatus) -> Mono<out Throwable>): Response {
		return SpringWebfluxResponse(requestHeaderSpec, statusHandlers + (status to handler), headerHandler, timingHandler, requestTimeout)
	}

	override fun onHeader(header: String, handler: (String) -> Mono<Unit>): Response {
		return SpringWebfluxResponse(requestHeaderSpec, statusHandlers, headerHandler + (header to handler), timingHandler, requestTimeout)
	}

	override fun withTiming(handler: (Long) -> Mono<Unit>): Response {
		return SpringWebfluxResponse(requestHeaderSpec, statusHandlers, headerHandler, handler, requestTimeout)
	}

	override fun <T> toMono(
		handler: (
			body: Flux<ByteBuffer>,
			statusCode: Int,
			headers: Map<String, List<String>>
		) -> Mono<T>,
	): Mono<T> {
		val start = System.currentTimeMillis()

		return Mono.deferContextual { ctx ->
			requestHeaderSpec.exchangeToMono { cr ->
				val statusCode: Int = cr.statusCode().value()

				val headers = cr.headers().asHttpHeaders()
				val flatHeaders = headers.flatMap { (k, vals) -> vals.map { v -> AbstractMap.SimpleEntry(k, v) } }

				val headerHandlers = if (headerHandler.isNotEmpty()) {
					headers.flatMap { (k, values) -> values.map { k to it } }.fold(Mono.empty()) { m: Mono<*>, (k, v) ->
						m.then(headerHandler[k]?.let { it(v) } ?: Mono.empty())
					}
				} else Mono.empty()

				headerHandlers.then(
					statusHandlers[statusCode]?.let { handler ->
						cr.bodyToMono(ByteBuffer::class.java).flatMap { byteBuffer ->
							val arr = ByteArray(byteBuffer.remaining())
							byteBuffer.get(arr)
							val res = handler(object : ResponseStatus(statusCode, flatHeaders) {
								override fun responseBodyAsString() = arr.toString(Charsets.UTF_8)
							})
							if (res == Mono.empty<Throwable>()) {
								handler(Flux.just(ByteBuffer.wrap(arr)), statusCode, headers)
							} else {
								res.flatMap { Mono.error(it) }
							}
						}.switchIfEmpty(
							handler(object : ResponseStatus(statusCode, flatHeaders) {
								override fun responseBodyAsString() = ""
							}).let { res ->
								if (res == Mono.empty<Throwable>()) {
									handler(Flux.just(ByteBuffer.wrap(ByteArray(0))), statusCode, headers)
								} else {
									res.flatMap { Mono.error(it) }
								}
							}
						)
					} ?: handler(
						cr.bodyToFlux(ByteBuffer::class.java),
						statusCode,
						headers
					)
				)
			}.doOnTerminate {
				timingHandler?.let { it(System.currentTimeMillis() - start).contextWrite(ctx).subscribe() }
			}
		}.onErrorMap {
			if (it is WebClientRequestException && it.cause is ReadTimeoutException) {
				throw TimeoutException(it)
			} else it
		}
	}

	override fun toFlux(): Flux<ByteBuffer> {
		val start = System.currentTimeMillis()

		return Flux.deferContextual { ctx -> requestHeaderSpec.exchangeToFlux { cr ->
			val statusCode: Int = cr.statusCode().value()

			val headers = cr.headers().asHttpHeaders()
			val flatHeaders = headers.flatMap { (k, vals) -> vals.map { v -> AbstractMap.SimpleEntry(k, v) } }

			val headerHandlers = if (headerHandler.isNotEmpty()) {
				headers.flatMap { (k, values) -> values.map { k to it } }.fold(Mono.empty()) { m: Mono<*>, (k, v) -> m.then(headerHandler[k]?.let { it(v) } ?: Mono.empty()) }
			} else Mono.empty()

			headerHandlers.thenMany(
				statusHandlers[statusCode]?.let { handler ->
					cr.bodyToMono(ByteBuffer::class.java).flatMapMany { byteBuffer ->
						val arr = ByteArray(byteBuffer.remaining())
						byteBuffer.get(arr)
						val res = handler(object : ResponseStatus(statusCode, flatHeaders) {
							override fun responseBodyAsString() = arr.toString(Charsets.UTF_8)
						})
						if (res == Mono.empty<Throwable>()) {
							Mono.just(ByteBuffer.wrap(arr))
						} else {
							res.flatMap { Mono.error(it) }
						}
					}.switchIfEmpty(
						handler(object : ResponseStatus(statusCode, flatHeaders) {
							override fun responseBodyAsString() = ""
						}).let { res ->
							if (res == Mono.empty<Throwable>()) {
								Mono.just(ByteBuffer.wrap(ByteArray(0)))
							} else {
								res.flatMap { Mono.error(it) }
							}
						}
					)
				} ?: cr.bodyToFlux(ByteBuffer::class.java)
			)
		}.doOnTerminate {
            timingHandler?.let { it(System.currentTimeMillis() - start).contextWrite(ctx).subscribe() }
        } }.onErrorMap {
			if (it is WebClientRequestException && it.cause is ReadTimeoutException) {
				throw TimeoutException(it)
			} else it
		}
	}
}

private fun HttpMethod?.toSpringMethod(): org.springframework.http.HttpMethod {
	return when (this) {
		HttpMethod.GET -> org.springframework.http.HttpMethod.GET
		HttpMethod.HEAD -> org.springframework.http.HttpMethod.HEAD
		HttpMethod.POST -> org.springframework.http.HttpMethod.POST
		HttpMethod.PUT -> org.springframework.http.HttpMethod.PUT
		HttpMethod.PATCH -> org.springframework.http.HttpMethod.PATCH
		HttpMethod.DELETE -> org.springframework.http.HttpMethod.DELETE
		HttpMethod.OPTIONS -> org.springframework.http.HttpMethod.OPTIONS
		null -> org.springframework.http.HttpMethod.GET
	}
}
