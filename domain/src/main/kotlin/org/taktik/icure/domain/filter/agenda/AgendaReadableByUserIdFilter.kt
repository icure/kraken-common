package org.taktik.icure.domain.filter.agenda

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Agenda

/**
 * Retrieves all the [Agenda]s where one of the [Agenda.rights] contains [userId].
 * As [Agenda] is not an encryptable entity, this filter requires no security precondition.
 */
interface AgendaReadableByUserIdFilter : Filter<String, Agenda> {
	val userId: String
}
