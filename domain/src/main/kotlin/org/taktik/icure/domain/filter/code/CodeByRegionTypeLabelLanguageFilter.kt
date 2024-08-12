/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.domain.filter.code

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.base.Code

/**
 * Retrieves all the codes of the provided [type] that either in [Code.label] or in [Code.searchTerms] have the
 * specified [language] and, for that language, at least a word that starts with [label].
 * In a single string, are considered different words the substring separated by the characters ' ', '|', '/' and '`'.
 * If [region] is not null, then the filter will return only the codes with that region in [Code.regions].
 */
interface CodeByRegionTypeLabelLanguageFilter : Filter<String, Code> {
	val region: String?
	val type: String
	val language: String
	val label: String?
}
