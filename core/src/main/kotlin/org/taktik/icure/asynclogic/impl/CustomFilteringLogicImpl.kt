package org.taktik.icure.asynclogic.impl

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.taktik.couchdb.ViewRow
import org.taktik.icure.asyncdao.GenericDAO
import org.taktik.icure.asynclogic.base.CustomFilteringLogic
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.entities.dao.IdWithValue
import org.taktik.icure.entities.filters.AbstractCustomFilter
import org.taktik.icure.entities.filters.ByKeysCustomFilter
import org.taktik.icure.entities.filters.ByRangeCustomFilter
import org.taktik.icure.pagination.PaginationElement
import org.taktik.icure.pagination.toPaginatedFlow

open class CustomFilteringLogicImpl(
	protected val dao: GenericDAO<*>,
	private val datastoreInstanceProvider: DatastoreInstanceProvider,
) : CustomFilteringLogic {

	override fun doMatchEntitiesByCustomFilterWithIdExtractor(
		datastoreInformation: IDatastoreInformation,
		filter: AbstractCustomFilter,
		extractId: (row: ViewRow<*, *, *>) -> String?,
	): Flow<PaginationElement> = flow {
		when (filter) {
			is ByKeysCustomFilter -> dao.listEntitiesIdInCustomView(
				datastoreInformation = datastoreInformation,
				viewName = filter.viewName,
				keyComponents = filter.keyComponents,
				startKey = filter.startKey,
				startDocumentId = filter.startDocumentId,
				limit = filter.limit + 1
			)
			is ByRangeCustomFilter -> dao.listEntitiesIdInCustomView(
				datastoreInformation = datastoreInformation,
				viewName = filter.viewName,
				nonRangeKeyComponents = filter.nonRangeKeyComponents,
				range = filter.range,
				startKey = filter.startKey,
				startDocumentId = filter.startDocumentId,
				limit = filter.limit + 1
			)
		}.toPaginatedFlow(
			pageSize = filter.limit,
			extractElement = { id, row ->
				(row.value as? JsonNode)?.let {
					IdWithValue(
						id = id,
						value = it
					)
				}
			},
			extractId = extractId,
		).let {
			emitAll(it)
		}
	}

	protected fun doMatchEntitiesByCustomFilter(
		datastoreInformation: IDatastoreInformation,
		filter: AbstractCustomFilter,
	): Flow<PaginationElement> = doMatchEntitiesByCustomFilterWithIdExtractor(
		datastoreInformation = datastoreInformation,
		filter = filter,
		extractId = { it.id }
	)

	override fun matchEntitiesByCustomFilter(filter: AbstractCustomFilter): Flow<PaginationElement> = flow {
		emitAll(
			doMatchEntitiesByCustomFilter(
				datastoreInformation = datastoreInstanceProvider.getInstanceAndGroup(),
				filter = filter,
			)
		)
	}

}