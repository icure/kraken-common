package org.taktik.icure.asynclogic.impl.filter.hcparty

import kotlinx.coroutines.flow.Flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.HealthcarePartyDAO
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.hcparty.HealthcarePartyByParentIdFilter
import org.taktik.icure.entities.HealthcareParty

@Service
@Profile("app")
class HealthcarePartyByParentIdFilter(
	private val healthcarePartyDAO: HealthcarePartyDAO,
) : Filter<String, HealthcareParty, HealthcarePartyByParentIdFilter> {
	override fun resolve(
		filter: HealthcarePartyByParentIdFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	): Flow<String> = healthcarePartyDAO.listHealthcarePartyIdsByParentId(
		datastoreInformation = datastoreInformation,
		parentId = filter.parentId,
	)
}
