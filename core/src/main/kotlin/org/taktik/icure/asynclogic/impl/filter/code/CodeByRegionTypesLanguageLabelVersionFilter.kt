package org.taktik.icure.asynclogic.impl.filter.code

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.transform
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.couchdb.ViewRowWithDoc
import org.taktik.icure.asyncdao.CodeDAO
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.code.CodeByRegionTypesLanguageLabelVersionFilter
import org.taktik.icure.entities.base.Code

@Service
@Profile("app")
class CodeByRegionTypesLanguageLabelVersionFilter(
	private val codeDAO: CodeDAO
): Filter<String, Code, CodeByRegionTypesLanguageLabelVersionFilter> {

	override fun resolve(
		filter: CodeByRegionTypesLanguageLabelVersionFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation
	): Flow<String> = flow {
		filter.types.forEach { type ->
			emitAll(findCodesByLabelInAllPages(
				datastoreInformation = datastoreInformation,
				region = filter.region,
				language = filter.language,
				type = type,
				label = filter.label,
				version = filter.version
			))
		}
	}

	suspend fun findCodesByLabelInAllPages(
		datastoreInformation: IDatastoreInformation,
		region: String?,
		language: String,
		type: String,
		label: String,
		version: String?,
		paginationOffset: PaginationOffset<List<String?>> = PaginationOffset(1000)
	): Flow<String> = flow {
		var emittedCount = 0
		var nextKey: ViewRowWithDoc<*, *, *>? = null
		emitAll(
			codeDAO.findCodesByLabel(
				datastoreInformation = datastoreInformation,
				region = region,
				language = language,
				type = type,
				label = label,
				version = version,
				paginationOffset = paginationOffset
			).filterIsInstance<ViewRowWithDoc<*, *, *>>().transform {
				if (emittedCount < paginationOffset.limit) {
					emittedCount++
					emit(it.id)
				} else if(emittedCount == paginationOffset.limit) {
					nextKey = it
				}
			}.onCompletion {
				if(nextKey != null && emittedCount >= paginationOffset.limit) {
					@Suppress("UNCHECKED_CAST")
					emitAll(findCodesByLabelInAllPages(
						datastoreInformation = datastoreInformation,
						region = region,
						language = language,
						type = type,
						label = label,
						version = version,
						paginationOffset = paginationOffset.copy(
							startDocumentId = nextKey?.id,
							startKey = nextKey?.key as List<String?>,
							limit = paginationOffset.limit
						),
					))
				} else if(nextKey != null) {
					emit(checkNotNull(nextKey).id)
				}
			}
		)
	}

}
