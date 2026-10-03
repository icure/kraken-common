package org.taktik.icure.entities.filters

import org.taktik.icure.entities.dao.KeyComponent
import org.taktik.icure.entities.dao.ValueFilterParameters

interface ByKeysCustomFilter : AbstractCustomFilter {
	val keyComponents: List<List<KeyComponent>>
	val valueFilterParameters: ValueFilterParameters?
}