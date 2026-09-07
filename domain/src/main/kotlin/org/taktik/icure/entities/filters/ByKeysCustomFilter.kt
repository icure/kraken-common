package org.taktik.icure.entities.filters

import org.taktik.icure.entities.dao.KeyComponent

interface ByKeysCustomFilter : AbstractCustomFilter {
	val keyComponents: List<List<KeyComponent<*>>>
}