package org.taktik.icure.domain.filter.impl.agenda

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.Filters
import org.taktik.icure.entities.Agenda
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class AllAgendasFilter(
	override val desc: String? = null,
) : AbstractFilter<Agenda>,
	Filters.AllFilter<String, Agenda> {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: Agenda, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean) = true
}
