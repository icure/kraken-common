/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.icure.annotations.entities.ContentValue
import org.taktik.icure.annotations.entities.ContentValues
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * @property rrule a RFC-5545 recurrence rule specifying the days and recurrence type of the timetable item. ("RRULE:FREQ=WEEKLY;UNTIL=20220930T150400Z;COUNT=30;INTERVAL=2;WKST=MO;BYDAY=TH" = every 2 weeks on Thursday until 30 September 2022.)
 * Note: The RFC-5545 rrule is used only to manage the days of the occurrences. The hours and durations of the appointments are specified in the property .hours.
 */
data class TimeTableItem(
	val rruleStartDate: Long? = null, // YYYYMMDD
	@param:ContentValue(ContentValues.ANY_STRING) val rrule: String? = null,

	/**
	 * If not null, the slot iterator will not provide slots before now() - [notBeforeInMinutes], where now is the
	 * current timestamp in minutes.
	 */
	@param:ContentValue(ContentValues.ANY_INT) val notBeforeInMinutes: Int? = null,
	/**
	 * If not null, the slot iterator will not provide slots after now() - [notBeforeInMinutes], where now is the
	 * current timestamp in minutes.
	 */
	@param:ContentValue(ContentValues.ANY_INT) val notAfterInMinutes: Int? = null,
	@param:ContentValue(ContentValues.ANY_STRING) val zoneId: String? = null,

	@Deprecated("Will be replaced by rrule") val days: List<String> = emptyList(),
	@Deprecated("Will be replaced by rrule") val recurrenceTypes: List<String> = emptyList(),
	@param:ContentValue(ContentValues.NESTED_ENTITIES_LIST) val hours: List<TimeTableHour> = emptyList(),
	@param:ContentValue(ContentValues.ANY_STRING) val calendarItemTypeId: String? = null,

	@param:ContentValue(ContentValues.ANY_BOOLEAN) @JsonProperty("isHomeVisit") val homeVisit: Boolean = false,
	@param:ContentValue(ContentValues.ANY_STRING) val placeId: String? = null,
	@param:ContentValue(ContentValues.ANY_BOOLEAN) val publicTimeTableItem: Boolean = false,
	@param:ContentValue(ContentValues.ANY_BOOLEAN) val acceptsNewPatient: Boolean = true,

	@JsonProperty("isUnavailable") val unavailable: Boolean = false
) : Serializable
