package org.taktik.icure.domain.filter.agenda

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Agenda
import org.taktik.icure.entities.base.PropertyStub

/**
 * Retrieves all the [Agenda]s that have at least one [Agenda.properties] that matches the id
 * and typedValue of the [property] passed as parameter.
 */
interface AgendaByTypedPropertyFilter : Filter<String, Agenda> {
	val property: PropertyStub
}