package org.taktik.icure.services.external.rest.v2.dto

import java.io.Serializable

data class PublicAgendasAndCalendarItemTypesDto(
	val agendas: List<AgendaDto>,
	val calendarItemTypes: List<CalendarItemTypeDto>,
) : Serializable
