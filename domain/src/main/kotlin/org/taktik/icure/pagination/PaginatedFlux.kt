package org.taktik.icure.pagination

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.reactive.FlowSubscription
import kotlinx.coroutines.reactor.asCoroutineContext
import org.taktik.icure.cache.ReactorCacheInjector
import reactor.core.CoreSubscriber
import reactor.core.publisher.Flux
import kotlin.coroutines.CoroutineContext

/**
 * A [Flux] of [PaginationElement]. Its purpose is to be recognized by the custom Jackson2Json encoder to serialize a
 * paginated list without collecting the flow of elements.
 * All the [PaginationRowElement] of this flux will contain an entity of type [T], this is necessary for the SDK code
 * generation.
 */
class PaginatedFlux<T>(
	private val flow: Flow<PaginationElement>,
	private val context: CoroutineContext,
	private val injector: ReactorCacheInjector? = null,
	private val cacheSize: Int? = null,
) : Flux<PaginationElement>() {

	@OptIn(InternalCoroutinesApi::class)
	override fun subscribe(subscriber: CoreSubscriber<in PaginationElement>) {
		val subscriberContext =
			if (injector != null && cacheSize != null && cacheSize > 0) {
				injector.injectCacheInContext(subscriber.currentContext().asCoroutineContext(), cacheSize)
			} else {
				subscriber.currentContext().asCoroutineContext()
			}

		subscriber.onSubscribe(FlowSubscription(flow.flowOn(subscriberContext), subscriber, context))
	}
}

/**
 * Converts a [Flow] of [PaginationElement] to a [PaginatedFlux], injecting the reactor context.
 *
 * @receiver a [Flow] of [PaginationElement].
 * @return a [PaginatedFlux].
 */
fun <T> Flow<PaginationElement>.asPaginatedFlux(): PaginatedFlux<T> = PaginatedFlux(this, Dispatchers.Default)

fun <T> Flow<PaginationElement>.asPaginatedFluxWithCoroutineCache(injector: ReactorCacheInjector, cacheSize: Int): PaginatedFlux<T> {
	require(cacheSize > 0)
	return PaginatedFlux(this, Dispatchers.Unconfined, injector, cacheSize)
}
