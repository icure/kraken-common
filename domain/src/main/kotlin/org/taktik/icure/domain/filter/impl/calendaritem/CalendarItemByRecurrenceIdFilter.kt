package org.taktik.icure.domain.filter.impl.calendaritem

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.calendaritem.CalendarItemByRecurrenceIdFilter
import org.taktik.icure.entities.CalendarItem
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class CalendarItemByRecurrenceIdFilter(
	override val recurrenceId: String,
	override val desc: String? = null,
) : AbstractFilter<CalendarItem>,
	CalendarItemByRecurrenceIdFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: CalendarItem, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean = item.recurrenceId == recurrenceId
}
