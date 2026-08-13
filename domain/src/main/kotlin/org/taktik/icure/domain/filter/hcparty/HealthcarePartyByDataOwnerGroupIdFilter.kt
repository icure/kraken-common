package org.taktik.icure.domain.filter.hcparty

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.HealthcareParty

/**
 * Retrieves all the [HealthcareParty] entities directly linked to the data owner group with id [dataOwnerGroupId],
 * through the legacy [HealthcareParty.parentId] or a [HealthcareParty.dataOwnerGroups] link. Only direct links
 * match: membership is not propagated through the group hierarchies.
 * This filter requires a special permission to be used.
 */
interface HealthcarePartyByDataOwnerGroupIdFilter : Filter<String, HealthcareParty> {
	val dataOwnerGroupId: String
}
