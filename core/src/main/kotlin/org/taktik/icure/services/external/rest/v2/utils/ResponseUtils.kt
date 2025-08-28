package org.taktik.icure.services.external.rest.v2.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.reactor.mono
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import reactor.core.publisher.Mono

/**
 * After a recent update to spring Mono<T?> is not properly including the content-type anymore in the response.
 *
 * This wraps nullable result in a ResponseEntity so that in case there is null the response will be empty but will
 * contain the return type.
 */
fun <T: Any> monoWrappingResponseToJson(
	block: suspend CoroutineScope.() -> T?
): Mono<ResponseEntity<T?>> = mono {
	ResponseEntity
		.ok()
		.contentType(MediaType.APPLICATION_JSON)
		.body(block())
}