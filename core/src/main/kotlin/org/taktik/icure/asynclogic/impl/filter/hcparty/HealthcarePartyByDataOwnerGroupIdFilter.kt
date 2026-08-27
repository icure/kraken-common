package org.taktik.icure.asynclogic.impl.filter.hcparty

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
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

	/**
	 * All direct children of [HealthcarePartyByDataOwnerGroupIdFilter.dataOwnerGroupId], membership-only — no
	 * restriction on the group's own effective link type. An earlier iteration of this filter also supported
	 * restricting by the group's effective type, but that required an extra DAO fetch of the group itself on every
	 * call for a filter of doubtful usefulness, so it was dropped.
	 */
	override fun resolve(
		filter: HealthcarePartyByDataOwnerGroupIdFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	): Flow<String> = flow {
		emitAll(
			healthcarePartyDAO.listHealthcarePartiesIdsByDataOwnerGroupId(
				datastoreInformation = datastoreInformation,
				dataOwnerGroupId = filter.dataOwnerGroupId,
			),
		)
	}
}
