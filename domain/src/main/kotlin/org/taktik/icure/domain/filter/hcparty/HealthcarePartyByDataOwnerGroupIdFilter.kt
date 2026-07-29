package org.taktik.icure.domain.filter.hcparty

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.base.DataOwnerGroupLinkType

/**
 * Retrieves all the [HealthcareParty] entities directly linked to the data owner group with id [dataOwnerGroupId],
 * through the legacy [HealthcareParty.parentId] (treated as a [DataOwnerGroupLinkType.parent] link) or a
 * [HealthcareParty.dataOwnerGroups] link. Only direct links match: membership is not propagated through the group
 * hierarchies.
 * This filter requires a special permission to be used.
 */
interface HealthcarePartyByDataOwnerGroupIdFilter : Filter<String, HealthcareParty> {
	val dataOwnerGroupId: String

	/**
	 * When not null, only healthcare parties whose effective link to the group has this type match. The effective
	 * link type is [DataOwnerGroupLinkType.parent] when the group is referenced by the legacy
	 * [HealthcareParty.parentId], and otherwise the type of the first [HealthcareParty.dataOwnerGroups] link
	 * pointing at the group.
	 */
	val linkType: DataOwnerGroupLinkType?
}
