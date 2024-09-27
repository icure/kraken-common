package org.taktik.icure.domain.filter.contact

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Contact

/**
 * Retrieves all the [Contact]s that have at least a Service in [Contact.services] with a code of type [codeType] and code
 * [codeCode] (if provided).
 * As this filter explicitly requires a data owner id, it does not need a security precondition.
 */
interface ContactByDataOwnerServiceCodeFilter : Filter<String, Contact> {
	val dataOwnerId: String
	val codeType: String
	val codeCode: String?
}