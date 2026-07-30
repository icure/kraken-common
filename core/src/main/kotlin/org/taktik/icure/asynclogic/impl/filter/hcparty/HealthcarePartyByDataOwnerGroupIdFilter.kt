package org.taktik.icure.asynclogic.impl.filter.hcparty

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.HealthcarePartyDAO
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.hcparty.HealthcarePartyByDataOwnerGroupIdFilter
import org.taktik.icure.entities.HealthcareParty

@Service
@Profile("app")
class HealthcarePartyByDataOwnerGroupIdFilter(
	private val healthcarePartyDAO: HealthcarePartyDAO,
) : Filter<String, HealthcareParty, HealthcarePartyByDataOwnerGroupIdFilter> {
	override val entity get() = healthcarePartyDAO.entityClass
	override val views = listOf("by_data_owner_group")

	override fun resolve(
		filter: HealthcarePartyByDataOwnerGroupIdFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	): Flow<String> = healthcarePartyDAO.listHealthcarePartiesIdsByDataOwnerGroupId(
		datastoreInformation = datastoreInformation,
		dataOwnerGroupId = filter.dataOwnerGroupId,
	).filter { (_, linkType) ->
		filter.linkType == null || filter.linkType == linkType
	}.map { (hcpId, _) -> hcpId }
}
