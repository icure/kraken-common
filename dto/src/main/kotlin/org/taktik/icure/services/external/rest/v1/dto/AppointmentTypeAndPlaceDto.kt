package org.taktik.icure.services.external.rest.v1.dto

import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v1.dto.embed.AddressDto

data class AppointmentTypeAndPlaceDto(
	val calendarItemTypeId: String,
	val name: String? = null,
	val color: String? = null, //"#123456"
	@Schema(defaultValue = "0") val duration: Int = 0,
	val subjectByLanguage: Map<String, String>? = emptyMap(),
	val placeId: String?,
	val address: AddressDto?,
	val acceptsNewPatients: Boolean = true,
)
