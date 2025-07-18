package org.taktik.icure.asynclogic.impl.filter.agenda

import kotlinx.coroutines.flow.Flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.AgendaDAO
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.agenda.AgendaReadableByUserRightsFilter
import org.taktik.icure.entities.Agenda

@Service
@Profile("app")
class AgendaReadableByUserRightsFilter(
	private val agendaDAO: AgendaDAO,
) : Filter<String, Agenda, AgendaReadableByUserRightsFilter> {
	override fun resolve(
		filter: AgendaReadableByUserRightsFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	): Flow<String> = agendaDAO.listReadableAgendaIdsByUserRights(datastoreInformation, filter.userId)
}
