package org.taktik.icure.domain.filter.impl.calendaritem

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.calendaritem.CalendarItemByDataOwnerUpdatedBetween
import org.taktik.icure.entities.CalendarItem
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class CalendarItemByDataOwnerUpdatedBetween(
	override val dataOwnerId: String,
	override val updatedAfter: Long?,
	override val updatedBefore: Long?,
	override val descending: Boolean,
	override val desc: String? = null
) : AbstractFilter<CalendarItem>, CalendarItemByDataOwnerUpdatedBetween {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = setOf(dataOwnerId)

	override fun matches(item: CalendarItem, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean =
		searchKeyMatcher(dataOwnerId, item)
			&& (item.created != null || item.modified != null || item.deletionDate != null)
			&& listOfNotNull(item.created, item.modified, item.deletionDate).max().let {
				(updatedAfter == null || it >= updatedAfter) && (updatedBefore == null || it <= updatedBefore)
			}

}
