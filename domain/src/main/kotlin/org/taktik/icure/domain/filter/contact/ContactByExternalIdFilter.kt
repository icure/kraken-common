package org.taktik.icure.domain.filter.contact

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Contact

/**
 * Returns all the [Contact]s where [Contact.externalId] is equal to [externalId].
 * As [Contact] is an encryptable entity but this filter does not specify any data owner id, a special permission is
 * needed to use this filter.
 */
interface ContactByExternalIdFilter : Filter<String, Contact> {
	val externalId: String
}
