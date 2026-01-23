/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.embed

import org.taktik.icure.entities.RawJson

/**
 * An interface for root entities that can be extended with additional custom fields.
 */
interface ExtendableRoot : Extendable {
	val extensionsVersion: Int?

	fun solveConflictsWith(other: ExtendableRoot) = super<Extendable>.solveConflictsWith(other) + mapOf(
		"extensionsVersion" to (this.extensionsVersion ?: other.extensionsVersion),
	)
}
