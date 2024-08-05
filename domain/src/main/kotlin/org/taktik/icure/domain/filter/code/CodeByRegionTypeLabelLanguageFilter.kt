/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.domain.filter.code

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.base.Code

/**
 * Retrieves the all the [Code]s of a certain [type] that match the provided [label] in a certain [language].
 * If a [region] is provided, only the codes for that region will be returned.
 */
interface CodeByRegionTypeLabelLanguageFilter : Filter<String, Code> {
	val region: String?
	val type: String
	val language: String
	val label: String?
}
