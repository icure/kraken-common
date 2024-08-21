/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.contact

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Contact

/**
 * Retrieves all the [Contact]s that either have a [Contact.tags] a code stub with type [tagType] and code [tagCode] or
 * a stub with type [codeType] and code [codeCode] in [Contact.tags].
 * The filter will return only  the [Contact]s with a delegation for [healthcarePartyId], or for the data owner making
 * the request if [healthcarePartyId] is null.
 * If [patientSecretForeignKeys] is not null, then only the [Contact]s with those keys in [Contact.secretForeignKeys]
 * will be returned.
 * If the [startOfContactOpeningDate] fuzzy date is not null, only the ids of the [Contact]s with a successive opening date will be returned.
 * If the [endOfContactOpeningDate] timestamp is not null, only the ids of the [Contact]s with a previous opening date will be returned.
 * As this filter explicitly requires a data owner id, it does not need a security precondition.
 */
interface ContactByHcPartyPatientTagCodeDateFilter : Filter<String, Contact> {
	val healthcarePartyId: String?
	@Deprecated("Use patientSecretForeignKeys instead")
	val patientSecretForeignKey: String?
	val patientSecretForeignKeys: List<String>?
	val tagType: String?
	val tagCode: String?
	val codeType: String?
	val codeCode: String?
	val startOfContactOpeningDate: Long?
	val endOfContactOpeningDate: Long?
}
