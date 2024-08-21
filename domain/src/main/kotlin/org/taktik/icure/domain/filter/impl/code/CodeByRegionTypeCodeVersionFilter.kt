package org.taktik.icure.domain.filter.impl.code

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.code.CodeByRegionTypeCodeVersionFilter
import org.taktik.icure.entities.base.Code
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.utils.LATEST_VERSION

data class CodeByRegionTypeCodeVersionFilter(
	override val region: String,
	override val type: String?,
	override val code: String?,
	override val version: String?,
	override val desc: String?
) : AbstractFilter<Code>, CodeByRegionTypeCodeVersionFilter {

	override val canBeUsedInWebsocket = version != LATEST_VERSION
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: Code, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean {
		if (version == LATEST_VERSION) {
			throw UnsupportedOperationException("This filter with version == $LATEST_VERSION not support the matches operation.")
		}
		return item.regions.contains(region)
			&& (type == null || item.type == type)
			&& (code == null || item.code == code)
			&& (version == null || item.version == version)
	}

}