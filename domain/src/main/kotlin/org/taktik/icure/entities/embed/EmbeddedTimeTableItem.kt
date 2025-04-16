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
data class EmbeddedTimeTableItem(
	override val rruleStartDate: Long? = null, // YYYYMMDD
	@param:ContentValue(ContentValues.ANY_STRING) override val rrule: String? = null,
	@param:ContentValue(ContentValues.ANY_INT) override val notBeforeInMinutes: Int? = null,
	@param:ContentValue(ContentValues.ANY_INT) override val notAfterInMinutes: Int? = null,
	/**
	 * If not null prevents unprivileged users from canceling or moving the calendar items linked to this time table
	 * item less than [lockedAfterInMinutes] minutes before its scheduled time.
	 */
	@param:ContentValue(ContentValues.ANY_INT) val lockedAfterInMinutes: Int? = null,
	@param:ContentValue(ContentValues.NESTED_ENTITIES_LIST) override val hours: List<TimeTableHour> = emptyList(),
	@param:ContentValue(ContentValues.ANY_STRING) override val calendarItemTypeId: String? = null,
) : Serializable, ITimeTableItem
