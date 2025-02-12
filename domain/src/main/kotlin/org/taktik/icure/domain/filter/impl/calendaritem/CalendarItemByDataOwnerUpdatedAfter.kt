package org.taktik.icure.domain.filter.impl.calendaritem

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.calendaritem.CalendarItemByDataOwnerUpdatedAfter
import org.taktik.icure.entities.CalendarItem
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class CalendarItemByDataOwnerUpdatedAfter(
	override val dataOwnerId: String,
	override val updatedAfter: Long,
	override val desc: String? = null
) : AbstractFilter<CalendarItem>, CalendarItemByDataOwnerUpdatedAfter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = setOf(dataOwnerId)

	override fun matches(item: CalendarItem, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean =
		searchKeyMatcher(dataOwnerId, item)
			&& (item.created != null || item.modified != null || item.deletionDate != null)
			&& listOfNotNull(item.created, item.modified, item.deletionDate).max() >= updatedAfter

}
