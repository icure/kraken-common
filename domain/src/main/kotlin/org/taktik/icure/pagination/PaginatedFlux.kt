package org.taktik.icure.pagination

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.reactive.FlowSubscription
import kotlinx.coroutines.reactor.asCoroutineContext
import reactor.core.CoreSubscriber
import reactor.core.publisher.Flux
import kotlin.coroutines.CoroutineContext

/**
 * A [Flux] of [PaginationElement]. Its purpose is to be recognized by the custom Jackson2Json encoder to serialize a
 * paginated list without collecting the flow of elements.
 */
class PaginatedFlux(
	private val flow: Flow<PaginationElement>,
	private val context: CoroutineContext
) : Flux<PaginationElement>() {

	@OptIn(InternalCoroutinesApi::class)
	override fun subscribe(subscriber: CoreSubscriber<in PaginationElement>) {
		val hasContext = !subscriber.currentContext().isEmpty
		val source = if (hasContext) flow.flowOn(subscriber.currentContext().asCoroutineContext()) else flow
		subscriber.onSubscribe(FlowSubscription(source, subscriber, context))
	}
}

/**
 * Converts a [Flow] of [PaginationElement] to a [PaginatedFlux], injecting the reactor context.
 *
 * @receiver a [Flow] of [PaginationElement].
 * @return a [PaginatedFlux].
 */
fun Flow<PaginationElement>.asPaginatedFlux(): PaginatedFlux = PaginatedFlux(this, Dispatchers.Main)