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
 * A [Flux] of [MultiKeyPaginationElement], the equivalent of [PaginatedFlux] for the searches that paginate over
 * multiple view keys at once. Its purpose is to be recognized by the custom Jackson2Json encoder to serialize a
 * paginated list without collecting the flow of elements.
 *
 * All the [MultiKeyPaginationElement.Row] of this flux contain an entity of type [T] and the keys of its
 * [MultiKeyPaginationElement.NextPage] are of type [K]; this is necessary for the SDK code generation.
 */
class MultiKeyPaginatedFlux<T, K>(
	private val flow: Flow<MultiKeyPaginationElement<T, K>>,
	private val context: CoroutineContext,
	private val injector: ReactorCacheInjector? = null,
	private val cacheSize: Int? = null,
) : Flux<MultiKeyPaginationElement<T, K>>() {

	@OptIn(InternalCoroutinesApi::class)
	override fun subscribe(subscriber: CoreSubscriber<in MultiKeyPaginationElement<T, K>>) {
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
 * Converts a [Flow] of [MultiKeyPaginationElement] to a [MultiKeyPaginatedFlux], injecting the reactor context.
 *
 * @receiver a [Flow] of [MultiKeyPaginationElement].
 * @return a [MultiKeyPaginatedFlux].
 */
fun <T, K> Flow<MultiKeyPaginationElement<T, K>>.asMultiKeyPaginatedFlux(): MultiKeyPaginatedFlux<T, K> =
	MultiKeyPaginatedFlux(this, Dispatchers.Default)

fun <T, K> Flow<MultiKeyPaginationElement<T, K>>.asMultiKeyPaginatedFluxWithCoroutineCache(
	injector: ReactorCacheInjector,
	cacheSize: Int,
): MultiKeyPaginatedFlux<T, K> {
	require(cacheSize > 0)
	return MultiKeyPaginatedFlux(this, Dispatchers.Unconfined, injector, cacheSize)
}
