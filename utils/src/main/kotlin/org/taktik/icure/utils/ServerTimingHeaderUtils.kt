package org.taktik.icure.utils

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.reactor.ReactorContext
import org.springframework.http.server.reactive.ServerHttpResponseDecorator
import org.springframework.web.server.ServerWebExchange
import org.taktik.icure.spring.filters.ArrivalTimeFilter
import reactor.netty.http.server.HttpServerResponse
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

const val SERVER_TIMING_HEADER: String = "Server-Timing"

/** Attribute holding the [ServerTimings] of a request, set by `ServerTimingFilter`. */
const val SERVER_TIMINGS_ATTRIBUTE: String = "com.icure.request.serverTimings"

/** Request header enabling HTTP trailers on the response: without it, no trailer is ever sent. */
const val ENABLE_TRAILERS_HEADER: String = "Enable-Trailers"

private const val FEEDBACK_LIMIT_HEADER = "X-Couch-Requests-Feedback-Limit"
private const val DEFAULT_FEEDBACK_LIMIT = 5

/** Whether the client asked for HTTP trailers on the response, by passing [ENABLE_TRAILERS_HEADER]. */
fun ServerWebExchange.areTrailersEnabled(): Boolean = request.headers.containsKey(ENABLE_TRAILERS_HEADER)

/**
 * The reactor-netty response behind the (possibly decorated) response of this exchange, or null when running
 * on something else than reactor-netty, e.g. a mock response in a unit test.
 */
fun ServerWebExchange.reactorNettyResponse(): HttpServerResponse? = try {
	ServerHttpResponseDecorator.getNativeResponse<Any>(response) as? HttpServerResponse
} catch (_: IllegalArgumentException) {
	null
}

/**
 * The server timing entries measured while handling a request, waiting to be written on the response.
 *
 * Entries measured before the response commits go out as regular `Server-Timing` headers. The ones measured
 * later - typically CouchDB requests made while the response body is already streaming - can only go out as
 * HTTP trailers, because response headers are frozen once the response is committed.
 */
class ServerTimings {
	private val entries = ConcurrentLinkedQueue<String>()
	private val collected = AtomicInteger(0)

	/**
	 * Records [entry], unless [limit] limited entries were already recorded for this request.
	 * A null [limit] records [entry] unconditionally, without counting it toward the limit of the others.
	 */
	fun add(entry: String, limit: Int?) {
		when {
			limit == null -> entries.add(entry)
			collected.incrementAndGet() <= limit -> entries.add(entry)
			else -> collected.decrementAndGet()
		}
	}

	/** Hands every entry recorded so far to [write] and forgets it, so an entry is never written twice. */
	fun drainTo(write: (String) -> Unit) {
		while (true) write(entries.poll() ?: return)
	}
}

fun ServerWebExchange.serverTimings(): ServerTimings? = attributes[SERVER_TIMINGS_ATTRIBUTE] as? ServerTimings

/**
 * Records a `Server-Timing` entry for [exchange]. Entries are capped per request by the feedback limit, unless
 * [limited] is false: filter timings use that, so they are not crowded out by the CouchDB requests they trigger.
 */
fun addServerTimingHeader(
	exchange: ServerWebExchange,
	name: String,
	duration: Long,
	methodCallStart: Long? = null,
	limited: Boolean = true,
) {
	val desc = methodCallStart?.let { start ->
		exchange.getAttribute<Long>(ArrivalTimeFilter.ARRIVAL_TIME)?.let { arrivalTime -> "mfs:${start - arrivalTime}" }
	}
	// `dur=` is required by the Server-Timing grammar: without it browsers parse the duration as 0.
	val entry = "$name;dur=$duration${desc?.let { ";desc=\"$it\"" } ?: ""}"
	val limit = if (limited) exchange.request.headers.getFirst(FEEDBACK_LIMIT_HEADER)?.toIntOrNull() ?: DEFAULT_FEEDBACK_LIMIT else null
	exchange.serverTimings()?.add(entry, limit) ?: addDirectly(exchange, entry, limit)
}

/** Fallback for exchanges that never went through `ServerTimingFilter`, e.g. in unit tests. */
private fun addDirectly(exchange: ServerWebExchange, entry: String, limit: Int?) {
	try {
		if (limit == null || (exchange.response.headers[SERVER_TIMING_HEADER]?.size ?: 0) < limit) {
			exchange.response.headers.add(SERVER_TIMING_HEADER, entry)
		}
	} catch (_: UnsupportedOperationException) {
		// The response is already committed and its headers are read-only: nothing we can do here.
	}
}

suspend fun addServerTimingHeader(
	name: String,
	duration: Long,
	methodCallStart: Long? = null,
	limited: Boolean = true,
) {
	currentCoroutineContext()[ReactorContext.Key]
		?.context
		?.getOrEmpty<ServerWebExchange>(ServerWebExchange::class.java)
		?.orElse(null)
		?.let { addServerTimingHeader(exchange = it, name = name, duration = duration, methodCallStart = methodCallStart, limited = limited) }
}
