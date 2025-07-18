package org.taktik.icure.asynclogic.impl.filter.agenda

import kotlinx.coroutines.flow.Flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.AgendaDAO
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.domain.filter.agenda.AgendaReadableByUserIdFilter
import org.taktik.icure.entities.Agenda

@Service
@Profile("app")
class AgendaReadableByUserIdFilter(
	private val agendaDAO: AgendaDAO
) : Filter<String, Agenda, AgendaReadableByUserIdFilter> {

	override fun resolve(
		filter: AgendaReadableByUserIdFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation
	): Flow<String> = agendaDAO.listReadableAgendaByUserLegacy(datastoreInformation, filter.userId)
}
