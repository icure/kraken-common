package org.taktik.icure.services.external.rest.v2.dto

import java.io.Serializable

/**
 * DTO containing publicly accessible agendas and their associated calendar item types,
 * used for public appointment booking.
 */
data class PublicAgendasAndCalendarItemTypesDto(
	/** The list of publicly available agendas. */
	val agendas: List<AgendaDto>,
	/** The list of calendar item types associated with the public agendas. */
	val calendarItemTypes: List<CalendarItemTypeDto>,
) : Serializable
