package org.taktik.icure.asynclogic.impl.filter.agenda

import kotlinx.coroutines.flow.Flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.AgendaDAO
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.agenda.AgendaWithPropertyFilter
import org.taktik.icure.entities.Agenda

@Service
@Profile("app")
data class AgendaWithPropertyFilter(
	private val agendaDAO: AgendaDAO,
) : Filter<String, Agenda, AgendaWithPropertyFilter> {
	override fun resolve(
		filter: AgendaWithPropertyFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	): Flow<String> = agendaDAO.listAgendasWithProperty(
		datastoreInformation = datastoreInformation,
		propertyId = filter.propertyId,
	)
}
