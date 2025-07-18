package org.taktik.icure.domain.filter.classification

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Classification

/**
 * Retrieves the ids of all the [Classification]s with a delegation for [dataOwnerId] and a set of [Classification.secretForeignKeys].
 * Only the ids of the Classifications where [Classification.created] is not null are returned and the results are sorted by
 * [Classification.created] in ascending or descending order according to the [descending] parameter.
 * If the [startDate] timestamp is not null, only the ids of the [Classification]s created after it will be returned.
 * If the [endDate] timestamp is not null, only the ids of the [Classification]s created before it will be returned.
 * As this filter explicitly requires a data owner id, it does not need a security precondition.
 */
interface ClassificationByDataOwnerPatientCreatedDateFilter : Filter<String, Classification> {
	val dataOwnerId: String
	val secretForeignKeys: Set<String>
	val startDate: Long?
	val endDate: Long?
	val descending: Boolean?
}
