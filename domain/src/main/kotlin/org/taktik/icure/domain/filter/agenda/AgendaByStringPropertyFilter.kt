package org.taktik.icure.domain.filter.agenda

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Agenda

/**
 * Retrieves all the [Agenda.id]s for the agendas that have at least one [Agenda.properties] where the id is
 * [propertyId] and the stringValue of the typedValue is equal to [propertyValue].
 */
interface AgendaByStringPropertyFilter : Filter<String, Agenda> {
	val propertyId: String
	val propertyValue: String
}