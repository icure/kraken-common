package org.taktik.icure.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.reactor.asCoroutineContext
import kotlinx.coroutines.reactor.asFlux
import org.taktik.icure.cache.ReactorCacheInjector
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.context.Context

/**
 * Injects the reactor context in a [Flow] and converts it to a [Flux].
 * Also, it instantiates for each type of entity a cache with maximum size equal to 2 times the batchSize passed as
 * parameter.
 * This cache lives is stored in each reactor context and is deleted after the completion of the function.
 *
 * @param cacheSize the maximum size of the cache for each type of entity. If data is batched the recommended cache size
 * is batchSize * 2. This way if the same element is needed in two different batches it may still be available.
 */
fun <T : Any> Flow<T>.injectCachedReactorContext(
	injector: ReactorCacheInjector,
	cacheSize: Int,
): Flux<T> {
	require(cacheSize > 0)
	return Mono.deferContextual { Mono.just(it) }.flatMapMany { ctxView ->
		this.flowOn(injector.injectCacheInContext(Context.of(ctxView).asCoroutineContext(), cacheSize)).asFlux()
	}
}
