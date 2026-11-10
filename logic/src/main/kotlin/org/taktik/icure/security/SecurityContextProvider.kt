package org.taktik.icure.security

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.reactor.ReactorContext
import org.springframework.security.core.context.SecurityContext
import reactor.core.publisher.Mono

suspend fun loadSecurityContext(): Mono<SecurityContext>? =
	currentCoroutineContext()[ReactorContext]
		?.context
		?.get<Mono<SecurityContext>>(SecurityContext::class.java)
