package org.taktik.icure.services.external.rest.v2.dto.embed

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.io.Serializable

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
	JsonSubTypes.Type(value = AgendaSlottingAlgorithmDto.FixedIntervals::class, name = "FixedIntervals"),
)
sealed interface AgendaSlottingAlgorithmDto {
	/**
	 * The appointments can be taken at fixed intervals of [intervalMinutes] starting from the beginning of the
	 * scheduled working hours for the available timetable item.
	 * # Example
	 * - Time table item scheduled 09:30-12:00, 12:45-14:00
	 * - Time table item availabilities 10:03-10:50, 13:00-13:30,
	 * - [intervalMinutes] = 10
	 * - Calendar item type duration = 15
	 * The returned slots for that time table item are 10:10, 10:20, 10:30, 13:05, 13:15
	 */
	data class FixedIntervals(
		val intervalMinutes: Int,
	) : AgendaSlottingAlgorithmDto,
		Serializable
}
