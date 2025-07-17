package org.taktik.icure.asynclogic.impl.filter.agenda

import kotlinx.coroutines.flow.Flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.AgendaDAO
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.domain.filter.agenda.AgendaByTypedPropertyFilter
import org.taktik.icure.entities.Agenda

@Service
@Profile("app")
data class AgendaByTypedPropertyFilter(
	private val agendaDAO: AgendaDAO
) : Filter<String, Agenda, AgendaByTypedPropertyFilter> {

	override fun resolve(
		filter: AgendaByTypedPropertyFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation
	): Flow<String> = agendaDAO.listAgendasIdsByTypedProperty(
		datastoreInformation = datastoreInformation,
		property = filter.property
	)

}