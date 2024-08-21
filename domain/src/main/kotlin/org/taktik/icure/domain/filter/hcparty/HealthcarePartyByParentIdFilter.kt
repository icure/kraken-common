package org.taktik.icure.domain.filter.hcparty

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.HealthcareParty

/**
 * Retrieves all the [HealthcareParty] entities where [HealthcareParty.parentId] is equal to [parentId].
 * This filter requires a special permission to be used.
 */
interface HealthcarePartyByParentIdFilter : Filter<String, HealthcareParty> {
	val parentId: String
}