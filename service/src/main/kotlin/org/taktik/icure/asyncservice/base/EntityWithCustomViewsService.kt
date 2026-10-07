package org.taktik.icure.asyncservice.base

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.entities.filters.AbstractCustomFilter
import org.taktik.icure.pagination.PaginationElement

interface EntityWithCustomViewsService {

	suspend fun requestedDataOwnerIds(filter: AbstractCustomFilter): Set<String>
	fun matchByCustomFilter(filter: AbstractCustomFilter): Flow<PaginationElement>

}