package org.taktik.icure.domain.filter.impl.timetable

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.timetable.TimeTableByPeriodAndAgendaIdFilter
import org.taktik.icure.entities.TimeTable
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class TimeTableByPeriodAndAgendaIdFilter(
	override val agendaId: String,
	override val startDate: Long? = null,
	override val endDate: Long? = null,
	override val descending: Boolean? = null,
	override val desc: String? = null
) : AbstractFilter<TimeTable>, TimeTableByPeriodAndAgendaIdFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = true
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: TimeTable, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean =
		item.agendaId == agendaId
			&& (item.startTime?.let { it < (endDate ?: Long.MAX_VALUE) } ?: true)
			&& (item.endTime?.let { it > (startDate ?: 0) } ?: true)

}
