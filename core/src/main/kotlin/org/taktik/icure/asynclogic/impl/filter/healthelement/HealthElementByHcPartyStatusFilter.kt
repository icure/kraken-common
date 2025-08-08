package org.taktik.icure.asynclogic.impl.filter.healthelement

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.HealthElementDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.healthelement.HealthElementByHcPartyStatusFilter
import org.taktik.icure.entities.HealthElement

@Service
@Profile("app")
class HealthElementByHcPartyStatusFilter(
	private val healthElementDAO: HealthElementDAO,
	private val sessionLogic: SessionInformationProvider,
) : Filter<String, HealthElement, HealthElementByHcPartyStatusFilter> {
	override fun resolve(
		filter: HealthElementByHcPartyStatusFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	) = flow {
		emitAll(
			healthElementDAO.listHealthElementIdsByHcPartyAndStatus(
				datastoreInformation = datastoreInformation,
				searchKeys = sessionLogic.getAllSearchKeysIfCurrentDataOwner(filter.hcPartyId),
				status = filter.status,
			),
		)
	}
}
