package org.taktik.icure.domain.filter.service

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.base.LinkQualification
import org.taktik.icure.entities.embed.Service

/**
 * Retrieves the [Service.id]s that have in [Service.qualifiedLinks] at least one value among [linkValues]. If
 * [linkQualification] is not null, then the [Service.id] will be returned only if the value corresponds to [linkQualification] in
 * the map.
 * As [Service] is an encryptable entity and this filer does not specify a data owner id, a special permission is
 * required to use this filter.
 */
interface ServiceByQualifiedLinkFilter : Filter<String, Service> {
	val linkValues: List<String>
	val linkQualification: LinkQualification?
}