package org.taktik.icure.asynclogic.impl.filter.pricing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.transform
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.couchdb.ViewRowWithDoc
import org.taktik.icure.asyncdao.TarificationDAO
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.db.PaginationOffset
import org.taktik.icure.domain.filter.pricing.PricingByRegionTypesLanguageLabelFilter
import org.taktik.icure.entities.Tarification

@Service
@Profile("app")
class PricingByRegionTypesLanguageLabelFilter(
	private val tarificationDAO: TarificationDAO,
) : Filter<String, Tarification, PricingByRegionTypesLanguageLabelFilter> {
	override fun resolve(
		filter: PricingByRegionTypesLanguageLabelFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	): Flow<String> = flow {
		filter.types.forEach { type ->
			emitAll(
				findPricingByLabelInAllPages(
					datastoreInformation = datastoreInformation,
					region = filter.region,
					language = filter.language,
					type = type,
					label = filter.label,
				),
			)
		}
	}

	suspend fun findPricingByLabelInAllPages(
		datastoreInformation: IDatastoreInformation,
		region: String?,
		language: String,
		type: String,
		label: String,
		paginationOffset: PaginationOffset<List<String?>> = PaginationOffset(1000),
	): Flow<String> = flow {
		var emittedCount = 0
		var nextKey: ViewRowWithDoc<*, *, *>? = null
		emitAll(
			tarificationDAO
				.findTarificationsByLabel(
					datastoreInformation = datastoreInformation,
					region = region,
					language = language,
					type = type,
					label = label,
					pagination = paginationOffset,
				).filterIsInstance<ViewRowWithDoc<*, *, *>>()
				.transform {
					if (emittedCount < paginationOffset.limit) {
						emittedCount++
						emit(it.id)
					} else if (emittedCount == paginationOffset.limit) {
						nextKey = it
					}
				}.onCompletion {
					if (nextKey != null && emittedCount >= paginationOffset.limit) {
						@Suppress("UNCHECKED_CAST")
						emitAll(
							findPricingByLabelInAllPages(
								datastoreInformation = datastoreInformation,
								region = region,
								language = language,
								type = type,
								label = label,
								paginationOffset =
								paginationOffset.copy(
									startDocumentId = nextKey?.id,
									startKey = nextKey?.key as List<String?>,
									limit = paginationOffset.limit,
								),
							),
						)
					} else if (nextKey != null) {
						emit(checkNotNull(nextKey).id)
					}
				},
		)
	}
}
