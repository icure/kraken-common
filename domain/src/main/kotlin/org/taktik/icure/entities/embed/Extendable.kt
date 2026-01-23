/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.embed

import org.taktik.icure.entities.RawJson

/**
 * An interface for entities that can be extended with additional custom fields.
 */
interface Extendable {
	val extensions: RawJson.JsonObject?

	fun solveConflictsWith(other: Extendable) = mapOf(
		"extensions" to (this.extensions ?: other.extensions),
	)
}
