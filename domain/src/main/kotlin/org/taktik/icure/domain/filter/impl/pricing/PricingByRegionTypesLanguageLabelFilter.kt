package org.taktik.icure.domain.filter.impl.pricing

import org.taktik.icure.db.sanitizeString
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.pricing.PricingByRegionTypesLanguageLabelFilter
import org.taktik.icure.entities.Tarification
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class PricingByRegionTypesLanguageLabelFilter(
	override val region: String? = null,
	override val types: List<String>,
	override val language: String,
	override val label: String,
	override val desc: String? = null,
) : AbstractFilter<Tarification>,
	PricingByRegionTypesLanguageLabelFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: Tarification, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean {
		val ss = sanitizeString(label)
		return ss != null &&
			(region == null || item.regions.contains(region)) &&
			types.contains(item.type) &&
			item.label?.get(language)?.let { s -> sanitizeString(s)?.contains(ss) } == true
	}
}
