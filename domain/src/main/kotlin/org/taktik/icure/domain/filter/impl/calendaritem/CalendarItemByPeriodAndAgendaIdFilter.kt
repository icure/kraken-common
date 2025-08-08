package org.taktik.icure.domain.filter.impl.calendaritem

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.calendaritem.CalendarItemByPeriodAndAgendaIdFilter
import org.taktik.icure.entities.CalendarItem
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class CalendarItemByPeriodAndAgendaIdFilter(
	override val agendaId: String,
	override val startTime: Long,
	override val endTime: Long,
	override val descending: Boolean? = null,
	override val desc: String? = null,
) : AbstractFilter<CalendarItem>,
	CalendarItemByPeriodAndAgendaIdFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: CalendarItem, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean = item.agendaId == agendaId &&
		(item.startTime != null && startTime <= item.startTime) &&
		(item.endTime == null || endTime >= item.endTime)
}
