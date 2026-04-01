/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * @property rrule a RFC-5545 recurrence rule specifying the days and recurrence type of the timetable item. ("RRULE:FREQ=WEEKLY;UNTIL=20220930T150400Z;COUNT=30;INTERVAL=2;WKST=MO;BYDAY=TH" = every 2 weeks on Thursday until 30 September 2022.)
 * Note: The RFC-5545 rrule is used only to manage the days of the occurrences. The hours and durations of the appointments are specified in the property .hours.
 */
data class TimeTableItem(
	val rruleStartDate: Long? = null, // YYYYMMDD
	val rrule: String? = null,

	/**
	 * If not null, the slot iterator will not provide slots before now() - [notBeforeInMinutes], where now is the
	 * current timestamp in minutes.
	 */
	val notBeforeInMinutes: Int? = null,
	/**
	 * If not null, the slot iterator will not provide slots after now() - [notBeforeInMinutes], where now is the
	 * current timestamp in minutes.
	 */
	val notAfterInMinutes: Int? = null,
	val zoneId: String? = null,

	@Deprecated("Will be replaced by rrule") val days: List<String> = emptyList(),
	@Deprecated("Will be replaced by rrule") val recurrenceTypes: List<String> = emptyList(),
	val hours: List<TimeTableHour> = emptyList(),
	val calendarItemTypeId: String? = null,

	@param:JsonProperty("isHomeVisit") val homeVisit: Boolean = false,
	val placeId: String? = null,
	val publicTimeTableItem: Boolean = false,
	val acceptsNewPatient: Boolean = true,
	/**
	 * A list of the ids of the users who are allowed to create a CalendarItem in this slot.
	 */
	val reservingRights: Set<String> = emptySet(),

	@param:JsonProperty("isUnavailable") val unavailable: Boolean = false,
) : Serializable
