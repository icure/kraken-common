package org.taktik.icure.domain.filter.impl.timetable

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.timetable.TimeTableByAgendaIdFilter
import org.taktik.icure.entities.TimeTable
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class TimeTableByAgendaIdFilter(
	override val agendaId: String,
	override val desc: String? = null,
) : AbstractFilter<TimeTable>,
	TimeTableByAgendaIdFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = true
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: TimeTable, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean = item.agendaId == agendaId
}
