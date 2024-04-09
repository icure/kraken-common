package org.taktik.icure.domain.filter.impl.user

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.User
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class UsersByPatientIdFilter(
	override val desc: String?,
	override val patientId: String
) : AbstractFilter<User>, org.taktik.icure.domain.filter.user.UsersByPatientIdFilter {

	override val requiresSecurityPrecondition: Boolean = true
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: User, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean {
		return item.patientId != null &&
				patientId == item.patientId
	}

}
