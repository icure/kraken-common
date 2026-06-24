package org.taktik.icure.utils

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.reactor.ReactorContext
import org.springframework.web.server.ServerWebExchange
import org.taktik.icure.spring.filters.ArrivalTimeFilter

suspend fun addServerTimingHeader(
	name: String,
	duration: Long,
	methodCallStart: Long? = null,
) {
	val we = currentCoroutineContext()[ReactorContext.Key]
		?.context
		?.getOrEmpty<ServerWebExchange>(ServerWebExchange::class.java)
		?.orElse(null)
	if (we?.response?.headers?.let { it::class.simpleName?.startsWith("ReadOnly") } == false) {
		val desc = if (methodCallStart != null) {
			we.getAttribute<Long>(ArrivalTimeFilter.ARRIVAL_TIME)?.let { arrivalTime ->
				"mfc:${methodCallStart - arrivalTime}"
			}
		} else {
			null
		}
		we.response.headers.add(
			"Server-Timing",
			"$name;$duration${desc?.let { ";desc=\"$it\"" } ?: ""}"
		)
	}
}
