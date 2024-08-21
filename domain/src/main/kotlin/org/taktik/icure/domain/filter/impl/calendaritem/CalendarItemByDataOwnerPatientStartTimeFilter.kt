package org.taktik.icure.domain.filter.impl.calendaritem

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.calendaritem.CalendarItemByDataOwnerPatientStartTimeFilter
import org.taktik.icure.entities.CalendarItem
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class CalendarItemByDataOwnerPatientStartTimeFilter(
	override val dataOwnerId: String,
	override val startDate: Long?,
	override val endDate: Long?,
	override val secretPatientIds: Set<String>,
	override val descending: Boolean?,
	override val desc: String? = null
) : AbstractFilter<CalendarItem>, CalendarItemByDataOwnerPatientStartTimeFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = setOf(dataOwnerId)

	override fun matches(item: CalendarItem, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean =
		searchKeyMatcher(dataOwnerId, item)
			&& item.secretForeignKeys.intersect(secretPatientIds).isNotEmpty()
			&& (startDate == null || (item.startTime != null && item.startTime >= startDate))
			&& (endDate == null || (item.startTime != null && item.startTime <= endDate))

}
