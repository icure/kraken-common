package org.taktik.icure.spring.filters

import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class ArrivalTimeFilter : WebFilter {
	override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void?> {
		exchange.attributes[ARRIVAL_TIME] = System.currentTimeMillis()
		return chain.filter(exchange)
	}

	companion object {
		const val ARRIVAL_TIME: String = "com.icure.request.arrivalTime"
	}
}