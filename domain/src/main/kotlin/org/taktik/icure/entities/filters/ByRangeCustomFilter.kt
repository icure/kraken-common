package org.taktik.icure.entities.filters

import org.taktik.icure.entities.dao.KeyComponent
import org.taktik.icure.entities.dao.RangeQueryParameters

interface ByRangeCustomFilter : AbstractCustomFilter {
	val nonRangeKeyComponents: List<List<KeyComponent>>
	val range: RangeQueryParameters
}