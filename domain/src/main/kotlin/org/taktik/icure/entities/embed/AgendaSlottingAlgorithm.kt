package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.io.Serializable

/**
 * Specifies how available slots for a time-table should be calculated
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
	JsonSubTypes.Type(value = AgendaSlottingAlgorithm.FixedIntervals::class, name = "FixedIntervals"),
)
sealed interface AgendaSlottingAlgorithm {
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
	) : AgendaSlottingAlgorithm,
		Serializable {
		init {
			require(intervalMinutes > 0) { "AgendaSlottingAlgorithm.FixedIntervals.intervalMinutes must be greater than 0" }
		}
	}

	/**
	 * TODO Just short explanation of aim, not exactly how it calculates, may change in future
	 *
	 * The time slots are calculated greedily based on:
	 * - Shortest calendar item type duration
	 * - Existing appointments
	 * - TimeTableItem hours
	 *
	 * For each span of time that is not occupied, the time-slots are returned at a fixed interval from the first
	 * available time. The interval between two time slots is equal to the duration of the shortest calendar item type
	 * that shares a time-table item with the
	 *
	 * Uses:
	 *
	 * TODO
	 *
	 * # Examples:
	 *
	 * ## Example 1:
	 * - Time table
	 *   - Time table item 1
	 *     - Hours: 09:00–10:10
	 *     - Calendar item types: t1 - 10 min, t2 - 20 min
	 * - No appointments scheduled
	 *   - t1 returns: [09:00, 09:10, 09:20, 09:30, 09:40, 09:50, 10:00]
	 *   - t2 returns: [09:00, 09:10, 09:20, 09:30, 09:40, 09:50]
	 * - Scheduled appointments: 09:00-09:10 (t1), 09:40-09:50 (t1)
	 *   - t1 returns: [09:10, 09:20, 09:30, 09:50, 10:00]
	 *   - t2 returns: [09:10, 09:20, 09:50]
	 *
	 * ## Example 2
	 * - Time table
	 *   - Time table item 1
	 *     - Hours: 09:00–10:30
	 *     - Calendar item types: t1 - 20 min, t2 - 30 min
	 * - No appointments scheduled
	 *   - t1 returns: [09:00, 09:20, 09:40, 10:00]
	 *   - t2 returns: [09:00, 09:20, 09:40, 10:00]
	 * - Scheduled appointments: 09:00-09:20 (t1)
	 *   - t1 returns: [09:20, 09:40, 10:00]
	 *   - t2 returns: [09:20, 09:40]
	 * - Scheduled appointments: 09:00-09:20 (t1), 09:40-10:10 (t2)
	 *   - t1 returns: [09:20, 10:10]
	 *   - t2 returns: []
	 *
	 * ## Example 3
	 * - Time table
	 *   - Time table item 1
	 *     - Hours: 09:00–11:00
	 *     - Calendar item types: t1 - 20 min, t2 - 40 min
	 *   - Time table item 2
	 *     - Hours: 09:00–11:00
	 *     - Calendar item types: t2 - 40 min, Type 3 - 30 min
	 * - Scheduled appointments: 09:00-09:30 (t3) 09:00-09:40 (t2), 09:40-10:20 (t2), 10:20-11:00 (t2)
	 *   - t1 returns [09:40, 10:00, 10:20, 10:40]
	 *   - t2 returns [09:30, 09:40, 10:00, 10:20]
	 *   - t3 returns [09:30, 10:00, 10:30]
	 * - Scheduled appointments: 09:00-09:20 (t1) 09:20-09:40 (t1)
	 *   - t1 returns [10:40, 10:00, 10:20, 10:40]
	 *   - t2 returns [09:00, 09:30, 09:40, 10:00, 10:20]
	 *   - t3 returns [09:00, 09:30, 10:00, 10:30]
	 * TODO?
	 */
// 	data object MinimizeGaps : TimeTableSlottingAlgorithm

	/**
	 * The time slots are calculated greedily based on:
	 * - Requested calendar item type duration
	 * - Existing appointments
	 * - TimeTableItem hours
	 *
	 * For each span of time that is not occupied, the time-slots are returned at a fixed interval from the first
	 * available time. The time interval is the duration of the calendar item type being requested
	 *
	 * Use cases:
	 *
	 * TODO
	 *
	 * # Examples:
	 *
	 * ## Example 1:
	 * - Time table
	 *   - Time table item 1
	 *     - Hours: 09:00–10:10
	 *     - Calendar item types: t1 - 10 min, t2 - 20 min
	 * - No appointments scheduled
	 *   - t1 returns: [09:00, 09:10, 09:20, 09:30, 09:40, 09:50, 10:00]
	 *   - t2 returns: [09:00, 09:20, 09:40]
	 * - Scheduled appointments: 09:00-09:10 (t1), 09:40-09:50 (t1)
	 *   - t1 returns: [09:10, 09:20, 09:30, 09:50, 10:00]
	 *   - t2 returns: [09:10, 09:50]
	 *
	 * ## Example 2
	 * - Time table
	 *   - Time table item 1
	 *     - Hours: 09:00–10:30
	 *     - Calendar item types: t1 - 20 min, t2 - 30 min
	 * - No appointments scheduled
	 *   - t1 returns: [09:00, 09:20, 09:40, 10:00]
	 *   - t2 returns: [09:00, 09:30, 10:00]
	 * - Scheduled appointments: 09:00-09:20 (t1)
	 *   - t1 returns: [09:20, 09:40, 10:00]
	 *   - t2 returns: [09:20, 09:50]
	 * - Scheduled appointments: 09:00-09:20 (t1), 09:50-10:20 (t2)
	 *   - t1 returns: [09:20, 10:00]
	 *     - if the appointment at 9:20 is taken as t1 there will be a gap between 9:40-9:50 where no appointment can be taken
	 *   - t2 returns: [09:20]
	 *
	 * TODO example 3
	 */
// 	data object MaximizeAvailabilities : TimeTableSlottingAlgorithm

	// TODO
// 	data object FixedSlotsByEachCalendarItemType : TimeTableSlottingAlgorithm
}
