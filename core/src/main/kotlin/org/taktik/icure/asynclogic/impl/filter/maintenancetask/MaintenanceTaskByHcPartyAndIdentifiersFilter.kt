package org.taktik.icure.asynclogic.impl.filter.maintenancetask

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.MaintenanceTaskDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.domain.filter.maintenancetask.MaintenanceTaskByHcPartyAndIdentifiersFilter
import org.taktik.icure.entities.MaintenanceTask
import org.taktik.icure.utils.getLoggedDataOwnerId
import javax.security.auth.login.LoginException

@Service
@Profile("app")
class MaintenanceTaskByHcPartyAndIdentifiersFilter(
	private val maintenanceTaskDAO: MaintenanceTaskDAO,
	private val sessionLogic: SessionInformationProvider
) : Filter<String, MaintenanceTask, MaintenanceTaskByHcPartyAndIdentifiersFilter> {

	override fun resolve(
        filter: MaintenanceTaskByHcPartyAndIdentifiersFilter,
        context: Filters,
        datastoreInformation: IDatastoreInformation
    ) = flow {
		try {
			emitAll(maintenanceTaskDAO.listMaintenanceTaskIdsByHcPartyAndIdentifier(
				datastoreInformation = datastoreInformation,
				searchKeys = sessionLogic.getAllSearchKeysIfCurrentDataOwner(filter.healthcarePartyId ?: getLoggedDataOwnerId(sessionLogic)),
				identifiers = filter.identifiers
			))
		} catch (e: LoginException) {
			throw IllegalArgumentException(e)
		}
	}
}
