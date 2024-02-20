/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)

/**
 * @property rrule a RFC-5545 recurrence rule specifying the days and recurrence type of the timetable item. ("RRULE:FREQ=WEEKLY;UNTIL=20220930T150400Z;COUNT=30;INTERVAL=2;WKST=MO;BYDAY=TH" = every 2 weeks on Thursday until 30 September 2022.)
 * Note: The RFC-5545 rrule is used only to manage the days of the occurrences. The hours and durations of the appointments are specified in the property .hours.
 */


/**
 * @property rrule a RFC-5545 recurrence rule specifying the days and recurrence type of the timetable item. ("RRULE:FREQ=WEEKLY;UNTIL=20220930T150400Z;COUNT=30;INTERVAL=2;WKST=MO;BYDAY=TH" = every 2 weeks on Thursday until 30 September 2022.)
 * Note: The RFC-5545 rrule is used only to manage the days of the occurrences. The hours and durations of the appointments are specified in the property .hours.
 */

data class TimeTableItemDto(
	val rruleStartDate: Long? = null, // YYYYMMDD
	val rrule: String? = null,
	val notBeforeInMinutes: Int? = null,
	val notAfterInMinutes: Int? = null,
	val zoneId: String? = null,

	@JsonInclude(JsonInclude.Include.NON_EMPTY) @Deprecated("Will be replaced by rrule") val days: List<String> = emptyList(),
	@JsonInclude(JsonInclude.Include.NON_EMPTY) @Deprecated("Will be replaced by rrule") val recurrenceTypes: List<String> = emptyList(),

	@JsonInclude(JsonInclude.Include.NON_EMPTY) val hours: List<TimeTableHourDto> = emptyList(),
	val calendarItemTypeId: String? = null,

	val homeVisit: Boolean = false,
	val placeId: String? = null,
	val publicTimeTableItem: Boolean = false,
	val acceptsNewPatient: Boolean = true,
	val unavailable: Boolean = false
) : Serializable
