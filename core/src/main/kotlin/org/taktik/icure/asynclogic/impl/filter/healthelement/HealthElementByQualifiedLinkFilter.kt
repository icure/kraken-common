package org.taktik.icure.asynclogic.impl.filter.healthelement

import kotlinx.coroutines.flow.Flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.HealthElementDAO
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.healthelement.HealthElementByQualifiedLinkFilter
import org.taktik.icure.entities.HealthElement

@Service
@Profile("app")
class HealthElementByQualifiedLinkFilter(
	private val healthElementDAO: HealthElementDAO,
) : Filter<String, HealthElement, HealthElementByQualifiedLinkFilter> {
	override val entity get() = healthElementDAO.entityClass
	override val views = listOf("by_linked_health_element_id")

	override fun resolve(
		filter: HealthElementByQualifiedLinkFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	): Flow<String> = healthElementDAO.listHealthElementIdsByQualifiedLink(
		datastoreInformation = datastoreInformation,
		linkedIds = filter.linkedIds,
		type = filter.type,
	)
}
