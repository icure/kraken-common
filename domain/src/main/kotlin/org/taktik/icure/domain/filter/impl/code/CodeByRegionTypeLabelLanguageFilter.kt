/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.domain.filter.impl.code

import java.util.Objects
import org.taktik.icure.db.sanitizeString
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.base.Code
import org.taktik.icure.entities.base.Encryptable

data class CodeByRegionTypeLabelLanguageFilter(
	override val desc: String? = null,
	override val region: String,
	override val type: String,
	override val language: String,
	override val label: String? = null
) : AbstractFilter<Code>, org.taktik.icure.domain.filter.code.CodeByRegionTypeLabelLanguageFilter {

	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is CodeByRegionTypeLabelLanguageFilter) return false
		return region == other.region &&
				type == other.type &&
				language == other.language &&
				label == other.label
	}

	override fun hashCode(): Int {
		return Objects.hash(region, type, language, label)
	}

	override fun matches(item: Code, searchKeyMatcher: (String, Encryptable) -> Boolean): Boolean {
		val ss = sanitizeString(label)
		return ss != null && (
			(region == null || item.regions.contains(region)) &&
				type == null &&
				if (language == null) item.label?.values?.any { s -> sanitizeString(s)?.contains(ss) == true } == true else item.label?.get(language)?.let { s -> sanitizeString(s)?.contains(ss) } == true
			)
	}
}
