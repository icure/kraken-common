package org.taktik.icure.domain.filter.impl.code

import org.taktik.icure.db.sanitizeString
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.code.CodeByRegionTypesLanguageLabelVersionFilter
import org.taktik.icure.entities.base.Code
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.utils.LATEST_VERSION

data class CodeByRegionTypesLanguageLabelVersionFilter(
	override val region: String? = null,
	override val types: List<String>,
	override val language: String,
	override val label: String,
	override val version: String? = null,
	override val desc: String? = null,
) : AbstractFilter<Code>,
	CodeByRegionTypesLanguageLabelVersionFilter {

	override val canBeUsedInWebsocket = version != LATEST_VERSION
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: Code, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean {
		if (version == LATEST_VERSION) {
			throw UnsupportedOperationException("This filter with version == $LATEST_VERSION not support the matches operation.")
		}
		val ss = sanitizeString(label)
		return ss != null &&
			(region == null || item.regions.contains(region)) &&
			types.contains(item.type) &&
			item.label?.get(language)?.let { s -> sanitizeString(s)?.contains(ss) } == true &&
			(version == null || item.version == version)
	}
}
