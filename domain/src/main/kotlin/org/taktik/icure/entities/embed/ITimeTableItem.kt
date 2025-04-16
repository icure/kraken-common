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
interface ITimeTableItem : Serializable {
	val rruleStartDate: Long? // YYYYMMDD
	val rrule: String?
	/**
	 * If not null prevents unprivileged users from booking slots slots of this time table item in advance of more than
	 * [notBeforeInMinutes] minutes.
	 * Use-case: prevent users with public access to the agenda from booking appointments that are too far in the
	 * future.
	 */
	val notBeforeInMinutes: Int?
	/**
	 * If not null prevents unprivileged users from booking slots of this time table item in advance of less than
	 * [notAfterInMinutes] minutes.
	 * Use-case: prevent users with public access to the agenda from booking appointments without long enough notice
	 * (too close to the appointment time).
	 */
	val notAfterInMinutes: Int?
	val hours: List<TimeTableHour>
	val calendarItemTypeId: String?
}
