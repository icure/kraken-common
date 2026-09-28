package org.taktik.icure.spring.filters

import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import org.taktik.icure.utils.SERVER_TIMINGS_ATTRIBUTE
import org.taktik.icure.utils.SERVER_TIMING_HEADER
import org.taktik.icure.utils.ServerTimings
import org.taktik.icure.utils.reactorNettyResponse
import reactor.core.publisher.Mono

/**
 * Collects the `Server-Timing` entries of a request and writes them on the response as late as possible.
 *
 * Response headers are frozen when the response commits, which for a streamed body happens on the first
 * emitted buffer - long before the last CouchDB request of that stream completes. Entries are therefore
 * buffered and written twice over: everything measured before the commit goes out as regular `Server-Timing`
 * headers, and everything measured afterwards goes out as `Server-Timing` trailers. Trailers only reach the
 * wire on a chunked HTTP/1.1 response or on HTTP/2, and only browser devtools read them - the Fetch API and
 * `PerformanceServerTiming` see the headers only.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
class ServerTimingFilter : WebFilter {
	override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
		val timings = ServerTimings()
		exchange.attributes[SERVER_TIMINGS_ATTRIBUTE] = timings

		exchange.response.beforeCommit {
			Mono.fromRunnable<Void> {
				timings.drainTo { entry -> exchange.response.headers.add(SERVER_TIMING_HEADER, entry) }
			}
		}

		exchange.reactorNettyResponse()?.let { nativeResponse ->
			exchange.response.headers.add(HttpHeaders.TRAILER, SERVER_TIMING_HEADER)
			nativeResponse.trailerHeaders { trailers ->
				timings.drainTo { entry -> trailers.add(SERVER_TIMING_HEADER, entry) }
			}
		}

		return chain.filter(exchange)
	}
}
