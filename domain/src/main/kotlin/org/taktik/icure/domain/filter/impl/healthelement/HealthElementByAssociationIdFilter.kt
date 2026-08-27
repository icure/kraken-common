package org.taktik.icure.domain.filter.impl.healthelement

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.healthelement.HealthElementByAssociationIdFilter
import org.taktik.icure.entities.HealthElement
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class HealthElementByAssociationIdFilter(
	override val associationId: String,
	override val desc: String? = null,
) : AbstractFilter<HealthElement>,
	HealthElementByAssociationIdFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = true
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: HealthElement, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean = item.qualifiedLinks.any { it.associationId == associationId }
}
