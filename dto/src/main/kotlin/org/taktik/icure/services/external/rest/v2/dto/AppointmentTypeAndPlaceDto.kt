package org.taktik.icure.services.external.rest.v2.dto

import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v2.dto.embed.AddressDto

/**
 * DTO representing an appointment type along with its associated place information.
 */
data class AppointmentTypeAndPlaceDto(
	/** The unique identifier of the calendar item type for this appointment. */
	@param:Schema(required = true) val calendarItemTypeId: String,
	/** The display name of the appointment type. */
	val name: String? = null,
	/** The color code associated with this appointment type, in hex format (e.g. "#123456"). */
	val color: String? = null, // "#123456"
	/** The default duration of this appointment type in minutes. */
	@param:Schema(defaultValue = "0") val duration: Int = 0,
	/** A map of appointment subjects keyed by language code. */
	@param:Schema(defaultValue = "emptyMap()") val subjectByLanguage: Map<String, String>? = emptyMap(),
	/** The unique identifier of the place where this appointment type is offered. */
	val placeId: String? = null,
	/** The address of the place where this appointment type is offered. */
	val address: AddressDto? = null,
	/** Whether this appointment type accepts new patients. */
	@param:Schema(defaultValue = "true") val acceptsNewPatients: Boolean = true,
)
