package org.taktik.icure.domain.filter.calendaritem

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.CalendarItem

/**
 * Retrieves all the [CalendarItem]s where [CalendarItem.recurrenceId] is equal to [recurrenceId].
 * As [CalendarItem] is an encryptable entity but this filter does not require any data owner id, it requires a security
 * precondition.
 */
interface CalendarItemByRecurrenceIdFilter : Filter<String, CalendarItem> {
	val recurrenceId: String
}
