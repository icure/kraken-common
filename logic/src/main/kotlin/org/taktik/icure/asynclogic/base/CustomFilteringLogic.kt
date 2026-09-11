package org.taktik.icure.asynclogic.base

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.ViewRow
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.filters.AbstractCustomFilter
import org.taktik.icure.pagination.PaginationElement

interface CustomFilteringLogic {

	fun matchEntitiesByCustomFilter(filter: AbstractCustomFilter): Flow<PaginationElement>

	/**
	 * This is public as it is used inside the ContactLogic / ContactCloudLogic to implement the filtering of Services
	 * by custom views.
	 */
	fun doMatchEntitiesByCustomFilterWithIdExtractor(
		datastoreInformation: IDatastoreInformation,
		filter: AbstractCustomFilter,
		extractId: (row: ViewRow<*, *, *>) -> String?
	): Flow<PaginationElement>
}