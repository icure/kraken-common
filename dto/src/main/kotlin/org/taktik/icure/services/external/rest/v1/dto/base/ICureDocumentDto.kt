/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.base

import io.swagger.v3.oas.annotations.media.Schema

interface ICureDocumentDto<T> :
	IdentifiableDto<T>,
	HasTagsDto,
	HasCodesDto {
	@get:Schema(
		description = "The timestamp (unix epoch in ms) of creation of this entity, will be filled automatically if missing. Not enforced by the application server.",
	)
	val created:
		Long?

	@get:Schema(
		description = "The date (unix epoch in ms) of the latest modification of this entity, will be filled automatically if missing. Not enforced by the application server.",
	)
	val modified:
		Long?

	@get:Schema(
		description = "The id of the User that has created this entity, will be filled automatically if missing. Not enforced by the application server.",
	)
	val author:
		String?

	@get:Schema(
		description = "The id of the HealthcareParty that is responsible for this entity, will be filled automatically if missing. Not enforced by the application server.",
	)
	val responsible:
		String?

	@get:Schema(description = "The id of the medical location where this entity was created.")
	val medicalLocationId: String?

	@get:Schema(description = "Soft delete (unix epoch in ms) timestamp of the object.")
	val endOfLife: Long?

}
