/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.base

import io.swagger.v3.oas.annotations.media.Schema

interface HasMedicalLocationDto {
	@get:Schema(description = "The id of the medical location where this entity was created.")
	val medicalLocationId: String?
}
