package org.taktik.icure.domain.filter.impl.agenda

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.agenda.AgendaReadableByUserRightsFilter
import org.taktik.icure.entities.Agenda
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class AgendaReadableByUserRightsFilter(
	override val userId: String,
	override val desc: String? = null,
) : AbstractFilter<Agenda>,
	AgendaReadableByUserRightsFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: Agenda, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean = item.userRights.keys.any { it == userId }
}
