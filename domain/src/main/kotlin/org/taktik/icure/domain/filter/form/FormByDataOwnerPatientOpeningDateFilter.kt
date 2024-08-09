package org.taktik.icure.domain.filter.form

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Form

/**
 * Retrieves all the [Form]s that the data owner with id [dataOwnerId] can access, where [Form.secretForeignKeys]
 * contains at least one of [secretPatientKeys] and where [Form.openingDate] is greater than or
 * equal to [startDate] (if provided) and less than or equal to [endDate] (if provided).
 * If [dataOwnerId] is the data owner making the request, then also the available secret access keys will be used to
 * retrieve the results.
 * If the [startDate] fuzzy date is null, all the [Form]s since the beginning of time will be returned.
 * If the [endDate] fuzzy date is null, all the [Form]s until the end of time will be returned.
 * The results will be sorted by [Form.openingDate] in ascending or descending order according to the value of the
 * [descending] parameter.
 * This filter explicitly requires a [dataOwnerId], so it does not require any security precondition.
 */
interface FormByDataOwnerPatientOpeningDateFilter : Filter<String, Form> {
	val dataOwnerId: String
	val startDate: Long?
	val endDate: Long?
	val secretPatientKeys: Set<String>
	val descending: Boolean?
}
