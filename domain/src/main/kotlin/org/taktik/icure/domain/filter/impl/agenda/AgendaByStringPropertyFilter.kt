package org.taktik.icure.domain.filter.impl.agenda

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.agenda.AgendaByStringPropertyFilter
import org.taktik.icure.entities.Agenda
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class AgendaByStringPropertyFilter(
	override val propertyId: String,
	override val propertyValue: String,
	override val desc: String? = null
) : AbstractFilter<Agenda>, AgendaByStringPropertyFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: Agenda, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean =
		item.properties.any {
			it.id == propertyId && it.typedValue?.stringValue == propertyValue
		}

}