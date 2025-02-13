package org.taktik.icure.domain.filter.calendaritem

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.CalendarItem

/**
 * Retrieves all the [CalendarItem]s with a delegation for [dataOwnerId], where the max among [CalendarItem.created],
 * [CalendarItem.modified], and [CalendarItem.deletionDate] is greater or equal than [updatedAfter] (if provided) and
 * less or equal than [updatedBefore] (if provided).
 * This filter explicitly requires a [dataOwnerId], so it does not require any security precondition.
 */
interface CalendarItemByDataOwnerUpdatedBetween : Filter<String, CalendarItem> {
	val dataOwnerId: String
	val updatedAfter: Long?
	val updatedBefore: Long?
	val descending: Boolean
}