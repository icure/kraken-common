package org.taktik.icure.domain.filter.contact

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Contact

/**
 * Retrieves the ids of all the [Contact]s with a delegation for [dataOwnerId] sorted by [Contact.openingDate] in
 * ascending or descending order according to the [descending] parameter.
 * If the [startDate] fuzzy date is not null, only the ids of the [Contact]s with a successive opening date will be returned.
 * If the [endDate] timestamp is not null, only the ids of the [Contact]s with a previous opening date will be returned.
 * As this filter explicitly requires a data owner id, it does not need a security precondition.
 */
interface ContactByDataOwnerOpeningDateFilter : Filter<String, Contact> {
	val dataOwnerId: String
	val startDate: Long?
	val endDate: Long?
	val descending: Boolean?
}