package org.taktik.icure.domain.filter.agenda

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Agenda

/**
 * Retrieves all the [Agenda]s of the agendas that have at least one [Agenda.properties] where the id matches
 * [propertyId].
 */
interface AgendaWithPropertyFilter : Filter<String, Agenda> {
	val propertyId: String
}
