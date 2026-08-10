package org.taktik.icure.domain.filter.healthelement

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.HealthElement

/**
 * Retrieves the [HealthElement]s that have in [HealthElement.qualifiedLinks], for any type of qualification, a link
 * with an association id equal to [associationId].
 * As [HealthElement] is an encryptable entity and this filter does not specify a data owner id, a special permission
 * is required to use this filter.
 */
interface HealthElementByAssociationIdFilter : Filter<String, HealthElement> {
	val associationId: String
}
