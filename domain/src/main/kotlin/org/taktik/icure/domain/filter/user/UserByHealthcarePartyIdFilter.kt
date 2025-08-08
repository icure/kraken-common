package org.taktik.icure.domain.filter.user

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.User

/**
 * Retrieves all the [User]s where [User.healthcarePartyId] is equal to [healthcarePartyId].
 */
interface UserByHealthcarePartyIdFilter : Filter<String, User> {
	val healthcarePartyId: String
}
