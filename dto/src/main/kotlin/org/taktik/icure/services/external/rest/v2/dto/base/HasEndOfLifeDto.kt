package org.taktik.icure.services.external.rest.v2.dto.base

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Interface for entities that support soft deletion through an end-of-life timestamp.
 */
interface HasEndOfLifeDto {

	@get:Schema(description = "Soft delete (unix epoch in ms) timestamp of the object.")
	val endOfLife: Long?

	fun solveConflictsWith(other: HasEndOfLifeDto): Map<String, Any?> = mapOf(
		"endOfLife" to (this.endOfLife?.coerceAtMost(other.endOfLife ?: Long.MAX_VALUE) ?: other.endOfLife)
	)
}