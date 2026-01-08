/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.domain.filter.healthelement

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.domain.filter.VersionFiltering
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.HealthElement

/**
 * Retrieves all the [HealthElement]s with a delegation for [healthcarePartyId], and that have a [HealthElement.tags] a
 * code stub with type [codeType] and code [codeCode].
 * If the [startOfHealthElementDate] fuzzy date time is not null, only the ids of the [HealthElement]s with a successive value date (inclusive, fallback to opening date if value date is not available) will be returned.
 * If the [endOfHealthElementDate] fuzzy date time  is not null, only the ids of the [HealthElement]s with a previous value date (inclusive, fallback to opening date if value date is not available) will be returned.
 * As this filter explicitly requires a data owner id, it does not need a security precondition.
 * The optional [versionFiltering] specifies if matching health elements must be returned only if they are a specific version.
 */
interface HealthElementByHcPartyCodeFilter : Filter<String, HealthElement> {
	val healthcarePartyId: String
	val codeType: String
	val codeCode: String
	val startOfHealthElementDate: Long?
	val endOfHealthElementDate: Long?
	val versionFiltering: VersionFiltering?
}
