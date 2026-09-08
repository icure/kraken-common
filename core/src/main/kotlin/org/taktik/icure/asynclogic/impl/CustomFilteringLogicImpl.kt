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
import org.taktik.icure.entities.dao.KeyComponent
import org.taktik.icure.entities.filters.AbstractCustomFilter
import org.taktik.icure.entities.filters.ByKeysCustomFilter
import org.taktik.icure.pagination.PaginationElement
import org.taktik.icure.pagination.toPaginatedFlow

open class CustomFilteringLogicImpl(
	protected val dao: GenericDAO<*>,
	private val datastoreInstanceProvider: DatastoreInstanceProvider,
) : CustomFilteringLogic {


	private fun JsonNode.toKeyComponent(): KeyComponent = when {
		isInt -> KeyComponent.Int(asInt())
		isLong -> KeyComponent.Long(asLong())
		isDouble || isFloat -> KeyComponent.Double(asDouble())
		isBoolean -> KeyComponent.Boolean(asBoolean())
		isTextual -> KeyComponent.String(textValue())
		else -> throw IllegalStateException("Invalid key type: $this")
	}

	private fun AbstractCustomFilter.toPaginationFilter(): suspend (row: ViewRow<*, *, *>) -> Boolean =
		if (this is ByKeysCustomFilter && valueFilterParameters != null) {
			{ row ->
				(row.value as? JsonNode)?.let { node ->
					when {
						// In CouchDB, null is before any value and so automatically excluded in the range.
						// Null can be a valid value in the view if the uses chose to allow it
						node.isNull -> null
						valueFilterParameters!!.valueKey != null && node.isObject ->
							node[valueFilterParameters!!.valueKey]?.toKeyComponent()
						!node.isObject && !node.isArray -> node.toKeyComponent()
						else -> throw IllegalStateException(
							"Cannot extract filtering value with key ${valueFilterParameters?.valueKey} from $node"
						)
					}?.let {
						it >= valueFilterParameters!!.range.startKey &&
							it <= valueFilterParameters!!.range.endKey
					} ?: false
				} ?: false
			}
		} else {
			{ true }
		}

	override fun doMatchEntitiesByCustomFilterWithIdExtractor(
		datastoreInformation: IDatastoreInformation,
		filter: AbstractCustomFilter,
		extractId: (row: ViewRow<*, *, *>) -> String?,
	): Flow<PaginationElement> = flow {
		dao.listEntitiesIdsInCustomView(
			datastoreInformation = datastoreInformation,
			filter = filter,
		).toPaginatedFlow(
			pageSize = filter.queryLimit,
			extractElement = { id, row ->
				(row.value as? JsonNode)?.let {
					IdWithValue(
						id = id,
						value = it
					)
				}
			},
			extractId = extractId,
			rowFilter = filter.toPaginationFilter()
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