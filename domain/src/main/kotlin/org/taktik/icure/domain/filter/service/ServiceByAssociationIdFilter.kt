package org.taktik.icure.domain.filter.service

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.embed.Service

/**
 * Retrieves the [Service]s that have in [Service.qualifiedLinks], for any type of qualification, an association
 * key equal to [associationId].
 * As [Service] is an encryptable entity and this filer does not specify a data owner id, a special permission is
 * required to use this filter.
 */
interface ServiceByAssociationIdFilter : Filter<String, Service> {
	val associationId: String
}