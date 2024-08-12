package org.taktik.icure.domain.filter.contact

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.embed.Identifier

/**
 * Retrieves the ids of all the [Contact.id]s with a delegation for [healthcarePartyId] and a set of [Contact.identifier].
 * If [healthcarePartyId] is null, then the current healthcare party id will be used instead.
 * All the [Contact]s that contains at least one of the [identifiers] will be returned.
 * As this filter explicitly requires a data owner id, it does not need a security precondition.
 */
interface ContactByHcPartyIdentifiersFilter : Filter<String, Contact> {
	val healthcarePartyId: String?
	val identifiers: List<Identifier>
}
