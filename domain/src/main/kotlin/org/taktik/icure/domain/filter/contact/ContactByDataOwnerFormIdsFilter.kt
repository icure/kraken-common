package org.taktik.icure.domain.filter.contact

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Contact

/**
 * Retrieves the ids of all the [Contact] with a delegation for [dataOwnerId] and a list of [formIds] that can be
 * present in the [Contact.subContacts] or in [Contact.services] of each contact.
 * This method will return a Contact if at least one of his sub-contacts or services has one of the specified
 * ids.
 * As this filter explicitly requires a data owner id, it does not need a security precondition.
 */
interface ContactByDataOwnerFormIdsFilter : Filter<String, Contact> {
	val dataOwnerId: String
	val formIds: List<String>
}
