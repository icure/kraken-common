package org.taktik.icure.domain.filter.impl.calendaritem

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.calendaritem.CalendarItemByPeriodAndDataOwnerIdFilter
import org.taktik.icure.entities.CalendarItem
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class CalendarItemByPeriodAndDataOwnerIdFilter(
	override val dataOwnerId: String,
	override val startTime: Long,
	override val endTime: Long,
	override val desc: String? = null
) : AbstractFilter<CalendarItem>, CalendarItemByPeriodAndDataOwnerIdFilter {

	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: CalendarItem, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean =
		searchKeyMatcher(dataOwnerId, item)
			&& (item.startTime != null && startTime <= item.startTime )
			&& (item.endTime != null && endTime >= item.endTime)

}