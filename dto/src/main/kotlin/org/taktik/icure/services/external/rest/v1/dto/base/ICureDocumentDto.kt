/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.base

import io.swagger.v3.oas.annotations.media.Schema

interface ICureDocumentDto<T> :
	IdentifiableDto<T>,
	HasTagsDto,
	HasCodesDto {
	@param:Schema(
		description = "The timestamp (unix epoch in ms) of creation of this entity, will be filled automatically if missing. Not enforced by the application server.",
	)
	val created:
		Long?

	@param:Schema(
		description = "The date (unix epoch in ms) of the latest modification of this entity, will be filled automatically if missing. Not enforced by the application server.",
	)
	val modified:
		Long?

	@param:Schema(
		description = "The id of the User that has created this entity, will be filled automatically if missing. Not enforced by the application server.",
	)
	val author:
		String?

	@param:Schema(
		description = "The id of the HealthcareParty that is responsible for this entity, will be filled automatically if missing. Not enforced by the application server.",
	)
	val responsible:
		String?

	@param:Schema(description = "The id of the medical location where this entity was created.")
	val medicalLocationId: String?

	@param:Schema(description = "Soft delete (unix epoch in ms) timestamp of the object.")
	val endOfLife: Long?

	fun solveConflictsWith(other: ICureDocumentDto<T>): Map<String, Any?> = mapOf(
		"id" to this.id,
		"created" to (this.created?.coerceAtMost(other.created ?: Long.MAX_VALUE) ?: other.created),
		"modified" to (this.modified?.coerceAtLeast(other.modified ?: 0L) ?: other.modified),
		"endOfLife" to (this.endOfLife?.coerceAtMost(other.endOfLife ?: Long.MAX_VALUE) ?: other.endOfLife),
		"author" to (this.author ?: other.author),
		"responsible" to (this.responsible ?: other.responsible),
		"medicalLocationId" to (this.medicalLocationId ?: other.medicalLocationId),
		"tags" to (other.tags + this.tags),
		"codes" to (other.codes + this.codes),
	)
}
