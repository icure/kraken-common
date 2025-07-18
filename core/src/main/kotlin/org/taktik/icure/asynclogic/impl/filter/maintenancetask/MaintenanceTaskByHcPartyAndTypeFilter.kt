/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl.filter.maintenancetask

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.MaintenanceTaskDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.domain.filter.maintenancetask.MaintenanceTaskByHcPartyAndTypeFilter
import org.taktik.icure.entities.MaintenanceTask
import org.taktik.icure.utils.getLoggedDataOwnerId
import org.taktik.icure.utils.mergeUniqueIdsForSearchKeys
import javax.security.auth.login.LoginException

@Service
@Profile("app")
class MaintenanceTaskByHcPartyAndTypeFilter(
	private val maintenanceTaskDAO: MaintenanceTaskDAO,
	private val sessionLogic: SessionInformationProvider
) : Filter<String, MaintenanceTask, MaintenanceTaskByHcPartyAndTypeFilter> {

	override fun resolve(
        filter: MaintenanceTaskByHcPartyAndTypeFilter,
        context: Filters,
        datastoreInformation: IDatastoreInformation
    ) = flow {
		try {
			val hcpId = filter.healthcarePartyId ?: getLoggedDataOwnerId(sessionLogic)
			mergeUniqueIdsForSearchKeys(sessionLogic.getAllSearchKeysIfCurrentDataOwner(hcpId)) { key ->
				maintenanceTaskDAO.listMaintenanceTaskIdsByHcPartyAndType(
					datastoreInformation = datastoreInformation,
					healthcarePartyId = key,
					type = filter.type,
					startDate = null,
					endDate = null
				)
			}.let { emitAll(it) }
		} catch (e: LoginException) {
			throw IllegalArgumentException(e)
		}
	}
}
