/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.domain.filter.healthelement

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.HealthElement

@Deprecated("""
	Use HealthElementByHcPartyCodeFilter, HealthElementByHcPartyTagFilter, or HealthElementByHcPartyStatusVersioningFilter instead.
	Equivalent if not specifying versionFiltering, or if using VersionFiltering.ANY, but uses new more efficient views.
	This filter is currently kept to allow groups that do not yet have the updated views to continue to work.
""")
interface HealthElementByHcPartyTagCodeFilter : Filter<String, HealthElement> {
	val healthcarePartyId: String
	val codeType: String?
	val codeCode: String?
	val tagType: String?
	val tagCode: String?
	val status: Int?
}
