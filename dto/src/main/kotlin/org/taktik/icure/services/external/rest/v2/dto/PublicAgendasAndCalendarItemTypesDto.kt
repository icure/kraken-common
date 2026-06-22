package org.taktik.icure.services.external.rest.v2.dto

import java.io.Serializable
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

/**
 * DTO containing publicly accessible agendas and their associated calendar item types,
 * used for public appointment booking.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.PublicAgendasAndCalendarItemTypesDto")
data class PublicAgendasAndCalendarItemTypesDto(
	/** The list of publicly available agendas. */
	@ActiveField val agendas: List<AgendaDto> = emptyList(),
	/** The list of calendar item types associated with the public agendas. */
	@ActiveField val calendarItemTypes: List<CalendarItemTypeDto> = emptyList(),
) : Serializable
