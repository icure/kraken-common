package org.taktik.icure.domain.filter.service

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.embed.Service
import org.taktik.icure.entities.embed.SubContact

/**
 * Retrieves all the [Service.id]s with a delegation for [healthcarePartyId], where
 * service belongs to a [SubContact] where [SubContact.healthElementId] is among the provided [healthElementIds].
 * This filter cannot be used in websocket as it needs to access the parent subContact of a service, which is not
 * possible for the match method.
 * As this filter explicitly specifies a data owner id, it does not require any security precondition to be used.
 */
interface ServiceByHcPartyHealthElementIdsFilter : Filter<String, Service> {
	val healthcarePartyId: String?
	val healthElementIds: List<String>
}
