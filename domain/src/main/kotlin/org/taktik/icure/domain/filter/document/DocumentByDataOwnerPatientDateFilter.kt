package org.taktik.icure.domain.filter.document

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Document
import java.time.Instant

/**
 * Retrieves all the [Document]s with a delegation for [dataOwnerId], where [Document.secretForeignKeys]
 * contains at least one of [secretPatientKeys] and where [Document.created] is greater than or
 * equal to [startDate] (if provided) and less than or equal to [endDate] (if provided).
 * If [startDate] is null, all the [Document]s since the beginning of time will be returned.
 * If [endDate] is null, all the [Document]s until the end of time will be returned.
 * The results will be sorted by [Document.created] in ascending or descending order according to the value of the
 * [descending] parameter.
 * This filter explicitly requires a [dataOwnerId], so it does not require any security precondition.
 */
interface DocumentByDataOwnerPatientDateFilter : Filter<String, Document> {
	val dataOwnerId: String
	val startDate: Instant?
	val endDate: Instant?
	val secretPatientKeys: Set<String>
	val descending: Boolean?
}
