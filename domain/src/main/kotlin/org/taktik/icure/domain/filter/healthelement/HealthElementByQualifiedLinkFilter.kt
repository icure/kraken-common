package org.taktik.icure.domain.filter.healthelement

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.HealthElement

/**
 * Retrieves the [HealthElement.id]s that have in [HealthElement.qualifiedLinks] at least one link towards one of
 * [linkedIds]. If [type] is not null, then only links with that qualification type are considered.
 * As [HealthElement] is an encryptable entity and this filter does not specify a data owner id, a special permission
 * is required to use this filter.
 */
interface HealthElementByQualifiedLinkFilter : Filter<String, HealthElement> {
	val linkedIds: List<String>
	val type: String?
}
