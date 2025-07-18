package org.taktik.icure.domain.filter.code

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.base.Code

/**
 * Retrieves all the [Code]s that have [linkType] as key in [Code.qualifiedLinks] and, if [linkedId] is specified, that
 * also have [linkedId] as one of the values for [linkType].
 */
interface CodeByQualifiedLinkFilter : Filter<String, Code> {
	val linkType: String
	val linkedId: String?
}
