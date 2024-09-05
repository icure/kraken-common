package org.taktik.icure.domain.filter.contact

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Contact

/**
 * Retrieves all the [Contact]s that have at least a Service in [Contact.services] with a tag of type [tagType] and code [tagCode]
 * (if provided).
 * As this filter explicitly requires a data owner id, it does not need a security precondition.
 */
interface ContactByDataOwnerServiceTagFilter : Filter<String, Contact> {
	val dataOwnerId: String
	val tagType: String
	val tagCode: String?
}