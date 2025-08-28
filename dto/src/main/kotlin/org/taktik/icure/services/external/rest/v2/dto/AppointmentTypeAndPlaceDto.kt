package org.taktik.icure.services.external.rest.v2.dto

import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v2.dto.embed.AddressDto

data class AppointmentTypeAndPlaceDto(
	@param:Schema(required = true) val calendarItemTypeId: String,
	val name: String? = null,
	val color: String? = null, // "#123456"
	@param:Schema(defaultValue = "0") val duration: Int = 0,
	@param:Schema(defaultValue = "emptyMap()") val subjectByLanguage: Map<String, String>? = emptyMap(),
	val placeId: String? = null,
	val address: AddressDto? = null,
	@param:Schema(defaultValue = "true") val acceptsNewPatients: Boolean = true,
)
