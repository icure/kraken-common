/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.service

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.embed.Service

/**
 * Retrieves all the [Service]s with a delegation for [healthcarePartyId] and, if [patientSecretForeignKeys], associated
 * to the patient owner of the key.
 * Also, the following conditions can be specified:
 * - If [tagType] is not null, then only the Services with a stub with that type in [Service.tags] will be returned.
 * - If [codeType] is not null, then only the Services with a stub with that code in [Service.tags] will be returned.
 *   This also required [tagType] not to be null.
 * - If [codeType] is not null, then only the Services with a stub with that type in [Service.codes] will be returned.
 * - If [codeCode] is not null, then only the Services with a stub with that code in [Service.codes] will be returned.
 *   This also required [codeType] not to be null.
 * - If [startValueDate] is specified, only the Services where [Service.valueDate] (or [Service.openingDate] if value
 *   date is null) is greater than or equal to [startValueDate] will be returned.
 * - If [endValueDate] is specified, only the Services where [Service.valueDate] (or [Service.openingDate] if value
 *   date is null) is less than or equal to [startValueDate] will be returned.
 * As this filter explicitly specifies a data owner id, it does not require any security precondition to be used.
 */
interface ServiceByHcPartyTagCodeDateFilter : Filter<String, Service> {
	val healthcarePartyId: String?
	@Deprecated("Use patientSecretForeignKeys instead")
	val patientSecretForeignKey: String?
	val patientSecretForeignKeys: List<String>?
	val tagType: String?
	val tagCode: String?
	val codeType: String?
	val codeCode: String?
	val startValueDate: Long?
	val endValueDate: Long?
	val descending: Boolean
}
