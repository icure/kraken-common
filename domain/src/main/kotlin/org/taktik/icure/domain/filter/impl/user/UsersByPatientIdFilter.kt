package org.taktik.icure.domain.filter.impl.user

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.User
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class UsersByPatientIdFilter(
	override val patientId: String,
	override val desc: String? = null,
) : AbstractFilter<User>,
	org.taktik.icure.domain.filter.user.UsersByPatientIdFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = true
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: User, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean = item.patientId != null &&
		patientId == item.patientId
}
