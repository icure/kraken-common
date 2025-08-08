package org.taktik.icure.domain.filter.impl.user

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.user.UserByHealthcarePartyIdFilter
import org.taktik.icure.entities.User
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class UserByHealthcarePartyIdFilter(
	override val healthcarePartyId: String,
	override val desc: String? = null,
) : AbstractFilter<User>,
	UserByHealthcarePartyIdFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = true
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: User, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean = item.healthcarePartyId != null && healthcarePartyId == item.healthcarePartyId
}
