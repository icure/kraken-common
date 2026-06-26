package org.taktik.icure.utils

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.reactor.ReactorContext
import org.springframework.http.HttpHeaders
import org.springframework.web.server.ServerWebExchange
import org.taktik.icure.spring.filters.ArrivalTimeFilter

fun HttpHeaders.isReadOnly() = this::class.simpleName?.startsWith("ReadOnly") == true

fun addServerTimingHeader(
	exchange: ServerWebExchange,
	name: String,
	duration: Long,
	methodCallStart: Long? = null,
) {
	val desc = if (methodCallStart != null) {
		exchange.getAttribute<Long>(ArrivalTimeFilter.ARRIVAL_TIME)?.let { arrivalTime ->
			"mfs:${methodCallStart - arrivalTime}"
		}
	} else {
		null
	}
	try {
		exchange.response.headers.add(
			"Server-Timing",
			"$name;$duration${desc?.let { ";desc=\"$it\"" } ?: ""}"
		)
	} catch (_: UnsupportedOperationException) { }

}

suspend fun addServerTimingHeader(
	name: String,
	duration: Long,
	methodCallStart: Long? = null,
) {
	val we = currentCoroutineContext()[ReactorContext.Key]
		?.context
		?.getOrEmpty<ServerWebExchange>(ServerWebExchange::class.java)
		?.orElse(null)
	if (we?.response?.headers?.isReadOnly() == false) {
		addServerTimingHeader(exchange = we, name = name, duration = duration, methodCallStart = methodCallStart)
	}
}
