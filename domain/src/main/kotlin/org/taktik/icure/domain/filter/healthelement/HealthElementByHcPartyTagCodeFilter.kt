/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.domain.filter.healthelement

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.HealthElement

/**
 * Retrieves all the [HealthElement]s that the data owner with id [healthcarePartyId] can access, and that have a [HealthElement.tags] a code stub with type [tagType] and code [tagCode] and/or
 * a stub with type [codeType] and code [codeCode] in [HealthElement.tags].
 *
 * As this filter explicitly requires a data owner id, it does not need a security precondition.
 *
 * If the status is not null, only the [HealthElement]s with the given [HealthElement.status] will be returned.
 */
interface HealthElementByHcPartyTagCodeFilter : Filter<String, HealthElement> {
	val healthcarePartyId: String
	val codeType: String?
	val codeCode: String?
	val tagType: String?
	val tagCode: String?
	val status: Int?
}
