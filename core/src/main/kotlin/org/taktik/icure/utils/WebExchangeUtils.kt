package org.taktik.icure.utils

import kotlinx.coroutines.reactor.ReactorContext
import org.springframework.web.server.ServerWebExchange
import kotlin.coroutines.coroutineContext

suspend fun addHeaderToExchange(
	header: String,
	data: () -> String,
) {
	coroutineContext[ReactorContext]
		?.context
		?.getOrEmpty<ServerWebExchange>(ServerWebExchange::class.java)
		?.orElse(null)
		?.let {
			it.response.headers.set(header, data())
		}
}
