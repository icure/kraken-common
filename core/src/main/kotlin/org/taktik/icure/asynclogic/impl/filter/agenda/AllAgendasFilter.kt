package org.taktik.icure.asynclogic.impl.filter.agenda

import kotlinx.coroutines.flow.Flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.AgendaDAO
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.Filters
import org.taktik.icure.entities.Agenda

@Service
@Profile("app")
class AllAgendasFilter(
	private val agendaDAO: AgendaDAO,
) : Filter<String, Agenda, Filters.AllFilter<String, Agenda>> {
	override fun resolve(
		filter: Filters.AllFilter<String, Agenda>,
		context: org.taktik.icure.asynclogic.impl.filter.Filters,
		datastoreInformation: IDatastoreInformation,
	): Flow<String> = agendaDAO.getEntityIds(datastoreInformation)
}
