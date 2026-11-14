package org.taktik.icure.asyncservice.base

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewQueryResultEvent
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.chain.FilterChain

/**
 * Defines the methods to search the entities [E] using filters, common to all the services of entities that can be
 * filtered.
 */
interface EntityWithFilterService<E : Identifiable<String>> {

	/**
	 * Retrieves the ids of the entities [E] matching the provided [filter].
	 *
	 * @param filter an [AbstractFilter] of [E].
	 * @return a [Flow] of the ids matching the filter.
	 */
	fun matchEntitiesBy(filter: AbstractFilter<E>): Flow<String>

	/**
	 * Retrieves a page of the entities [E] matching the provided [filter]. The entities that the current user
	 * cannot access are ignored.
	 *
	 * @param paginationOffset a [PaginationOffset] that defines the page.
	 * @param filter a [FilterChain] of [E].
	 * @return a [Flow] of [ViewQueryResultEvent]s containing the entities of the page.
	 */
	fun filterEntitiesBy(paginationOffset: PaginationOffset<Nothing>, filter: FilterChain<E>): Flow<ViewQueryResultEvent>
}
