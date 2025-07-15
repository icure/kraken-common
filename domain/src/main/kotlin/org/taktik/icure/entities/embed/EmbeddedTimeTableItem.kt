/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.dmfs.rfc5545.recur.InvalidRecurrenceRuleException
import org.dmfs.rfc5545.recur.RecurrenceRule
import org.taktik.icure.utils.FuzzyDates
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
data class EmbeddedTimeTableItem(
	/**
	 * Specifies the time when the rrule starts applying.
	 * Must be a (local) fuzzy date.
	 */
	val rruleStartDate: Int? = null,
	/**
	 * A RFC-5545 recurrence rule specifying the days and recurrence type of the timetable item.
	 * Example: "FREQ=WEEKLY;UNTIL=20220930;INTERVAL=2;WKST=MO;BYDAY=TH" means every 2 weeks on Thursday until 30
	 * September 2022.
	 *
	 * This must be only the RRULE property value, without the property name ("RRULE:FREQ=WEEKLY;BYDAY=TH" won't be
	 * accepted, it should be "FREQ=WEEKLY;BYDAY=TH" instead)
	 *
	 * If UNTIL is provided, it must be a local date, without time or zone information. This is because:
	 * - the hours and durations of the schedule are specified in the property [hours].
	 * - all times of the agenda schedule are to be considered as local
	 */
	val rrule: String,
	/**
	 * If not null prevents unprivileged users from booking slots of this time table item in advance of more than
	 * [notBeforeInMinutes] minutes.
	 * Use-cases:
	 * - Prevent users with public access to the agenda from booking appointments that are too far in the future
	 * - Leave some consultation slots available at the last minute, to provide some slots for emergencies.
	 */
	val notBeforeInMinutes: Int? = null,
	/**
	 * If not null prevents unprivileged users from booking slots of this time table item in advance of less than
	 * [notAfterInMinutes] minutes.
	 * Use-cases:
	 * - Prevent users with public access to the agenda from booking appointments without long enough notice (too close to the appointment time).
	 */
	val notAfterInMinutes: Int? = null,
	/**
	 * Hours while the timetable is active.
	 * All hours are local time.
	 *
	 * # Representing end of day
	 *
	 * To represent an hour that ends at the end of the day use the special hour 23_59_60
	 *
	 * # Representing overnight hours
	 *
	 * To represent overnight hours you must use two separate hours, one starting at 0 and ending at the end of the
	 * shift, the other starting at the start of the shift and ending at 23_59_60.
	 *
	 * For example, to represent a shift from 22 to 3 you use the following hours: [(0, 3_00_00), (22_00_00, 23_59_60)]
	 *
	 * # No overlapping hours
	 *
	 * Excluding the special case for overnight shifts you can't have overlapping hours:
	 * - [(9_00_00, 12_00_00), (12_00_00, 14_00_00)] -> should be replaced by [(9_00_00, 14_00_00)]
	 * - [(12_00_00, 16_00_00), (15_00_00, 18_00_00)] -> should be replaced by [(12_00_00, 18_00_00)]
	 */
	val hours: List<EmbeddedTimeTableHour>,
	/**
	 * Types of calendar items that can be serviced by this TimeTable
	 */
	val calendarItemTypesIds: Set<String>,
	/**
	 * How many calendar items this TimeTable can service in parallel.
	 * The default is 1
	 */
	val availabilities: Int = 1,
	/**
	 * The ids of the users who are allowed to create a CalendarItem in this slot.
	 * Note that this value will be ignored if [public] is true.
	 */
	val reservingRights: Set<String> = emptySet(),
	/**
	 * If true all users, even anonymous, can see availabilities for this item, and all registered user can take
	 * appointments for it.
	 */
	val public: Boolean = false,
) : Serializable {
	init {
		val rrule = try {
			RecurrenceRule(rrule, RecurrenceRule.RfcMode.RFC5545_STRICT)
		} catch (e: InvalidRecurrenceRuleException) {
			throw IllegalArgumentException("Invalid recurrence rule: $rrule - ${e.message}", e)
		}
		rrule.until?.also {
			require(it.isFloating && it.isAllDay) { "Unsupported UNTIL: must be a local date, without time or zone information" }
		}
		require(rruleStartDate == null || FuzzyDates.getFullLocalDate(rruleStartDate) != null) { "rruleStartDate must be a valid fuzzy date if provided" }
		require(notBeforeInMinutes == null || notBeforeInMinutes > 0) { "notBeforeInMinutes must be positive if provided" }
		require(notAfterInMinutes == null || notAfterInMinutes >= 0) { "notAfterInMinutes must be positive if provided" }
		require(hours.isNotEmpty()) { "Timetable item hours can't be empty" }
		// Check no overlap
		val sortedHours = hours.sortedBy { it.startHour }
		for (i in 1 until sortedHours.size) {
			val current = sortedHours[i]
			val previous = sortedHours[i - 1]

			require (current.startHour > previous.endHour) {
				"Time table item can't have overlapping hours"
			}
		}
	}
}