package org.taktik.icure.cache

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.reactor.ReactorContext
import reactor.core.publisher.Mono
import kotlin.coroutines.CoroutineContext

interface ReactorCacheInjector {

	/**
	 * Injects a local cache into the [ReactorContext] passed as parameter and converts it to a [CoroutineContext].
	 *
	 * @param ctx the [ReactorContext] where to inject the cache.
	 * @param cacheSize the size of the cache. Note: it is required that size is greater than 0.
	 * @return a [CoroutineContext] with the cache.
	 * @throws IllegalArgumentException if [cacheSize] of the cache is less or equal to 0.
	 */
	fun injectCacheInContext(ctx: ReactorContext, cacheSize: Int): CoroutineContext

	/**
	 * Creates a new [Mono], with a cache with the size passed as parameter.
	 * This cache lives is stored in each reactor context and is deleted after the completion of the function.
	 *
	 * @param cacheSize the maximum size of the cache for each type of entity. If data is batched the recommended cache size
	 * is batchSize * 2. This way if the same element is needed in two different batches it may still be available.
	 * @param block the body of the mono.
	 * @return a [Mono] of [T]
	 * @throws IllegalArgumentException if [cacheSize] is less or equal than 0.
	 */
	fun <T> monoWithCachedContext(cacheSize: Int, block: suspend CoroutineScope.() -> T?): Mono<T>
}
