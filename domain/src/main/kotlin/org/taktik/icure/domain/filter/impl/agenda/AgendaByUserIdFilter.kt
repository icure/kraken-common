package org.taktik.icure.domain.filter.impl.agenda

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.Agenda
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.domain.filter.agenda.AgendaByUserIdFilter as IAgendaByUserIdFilter

data class AgendaByUserIdFilter(
	override val userId: String,
	override val desc: String? = null,
) : AbstractFilter<Agenda>,
	IAgendaByUserIdFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: Agenda, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean = item.userId == userId
}
