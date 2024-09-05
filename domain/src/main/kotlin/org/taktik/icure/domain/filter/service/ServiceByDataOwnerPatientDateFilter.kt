package org.taktik.icure.domain.filter.service

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.embed.Service

/**
 * Retrieves the ids of all the [Service]s in a [Contact] with a delegation for [dataOwnerId] and a set of [Contact.secretForeignKeys].
 * If the [startDate] fuzzy date is not null, only the [Service]s where [Service.valueDate] (or [Service.openingDate] if
 * the value date is null) is greater than or equal to [startDate] will be returned.
 * If the [endDate] fuzzy date is not null, only the [Service]s where [Service.valueDate] (or [Service.openingDate] if
 * the value date is null) is less than or equal to [startDate] will be returned.
 * The results will be sorted by [Service.valueDate] (or [Service.openingDate]) in descending or ascending order
 * according to [descending].
 * As this filter explicitly requires a data owner id, it does not need a security precondition.
 */
interface ServiceByDataOwnerPatientDateFilter : Filter<String, Service> {
	val dataOwnerId: String
	val secretForeignKeys: Set<String>
	val startDate: Long?
	val endDate: Long?
	val descending: Boolean?
}