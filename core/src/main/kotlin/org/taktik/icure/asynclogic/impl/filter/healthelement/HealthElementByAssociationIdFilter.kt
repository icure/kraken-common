package org.taktik.icure.asynclogic.impl.filter.healthelement

import kotlinx.coroutines.flow.Flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.HealthElementDAO
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.healthelement.HealthElementByAssociationIdFilter
import org.taktik.icure.entities.HealthElement

@Service
@Profile("app")
class HealthElementByAssociationIdFilter(
	private val healthElementDAO: HealthElementDAO,
) : Filter<String, HealthElement, HealthElementByAssociationIdFilter> {
	override val entity get() = healthElementDAO.entityClass
	override val views = listOf("by_association_id")

	override fun resolve(
		filter: HealthElementByAssociationIdFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	): Flow<String> = healthElementDAO.listHealthElementIdsByAssociationId(
		datastoreInformation = datastoreInformation,
		associationId = filter.associationId,
	)
}
