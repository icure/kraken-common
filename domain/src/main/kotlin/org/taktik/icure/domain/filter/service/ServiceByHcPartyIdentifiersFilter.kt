package org.taktik.icure.domain.filter.service

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.embed.Identifier
import org.taktik.icure.entities.embed.Service

/**
 * Retrieves all the [Service]s that have a delegation to [healthcarePartyId] and that have at least
 * one of the provided [identifiers] in [Service.identifier].
 * As this filter explicitly specifies a data owner id, it does not require any security precondition to be used.
 */
interface ServiceByHcPartyIdentifiersFilter : Filter<String, Service> {
	val healthcarePartyId: String?
	val identifiers: List<Identifier>
}
