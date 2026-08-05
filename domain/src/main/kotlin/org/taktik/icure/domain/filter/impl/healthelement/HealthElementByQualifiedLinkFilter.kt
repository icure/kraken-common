package org.taktik.icure.domain.filter.impl.healthelement

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.healthelement.HealthElementByQualifiedLinkFilter
import org.taktik.icure.entities.HealthElement
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class HealthElementByQualifiedLinkFilter(
	override val linkedIds: List<String>,
	override val type: String? = null,
	override val desc: String? = null,
) : AbstractFilter<HealthElement>,
	HealthElementByQualifiedLinkFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = true
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: HealthElement, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean = item.qualifiedLinks.any { link ->
		(type == null || link.type == type) && linkedIds.contains(link.healthElementId)
	}
}
