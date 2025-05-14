package org.taktik.icure.domain.filter.impl.agenda

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.agenda.AgendaByTypedPropertyFilter
import org.taktik.icure.entities.Agenda
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.base.PropertyStub

data class AgendaByTypedPropertyFilter(
	override val property: PropertyStub,
	override val desc: String? = null
) : AbstractFilter<Agenda>, AgendaByTypedPropertyFilter {

	init {
		requireNotNull(property.id) { "Property in AgendaByTypedPropertyFilter has null id" }
		requireNotNull(
			property.typedValue?.stringValue
				?: property.typedValue?.booleanValue
				?: property.typedValue?.stringValue
				?: property.typedValue?.doubleValue
		) { "Property in AgendaByTypedPropertyFilter has null value" }
	}

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: Agenda, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean =
		item.properties.any {
			it.id == property.id && it.matchesTypedValueOf(property)
		}

	private fun PropertyStub.matchesTypedValueOf(other: PropertyStub): Boolean =
		(typedValue?.stringValue != null && other.typedValue?.stringValue == typedValue.stringValue)
			|| (typedValue?.doubleValue != null && other.typedValue?.doubleValue == typedValue.doubleValue)
			|| (typedValue?.booleanValue != null && other.typedValue?.booleanValue == typedValue.booleanValue)
			|| (typedValue?.integerValue != null && other.typedValue?.integerValue == typedValue.integerValue)
}