package org.taktik.icure.asynclogic.impl.filter.maintenancetask

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.MaintenanceTaskDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.maintenancetask.MaintenanceTaskByHcPartyAndIdentifiersFilter
import org.taktik.icure.entities.MaintenanceTask
import javax.security.auth.login.LoginException

@Service
@Profile("app")
class MaintenanceTaskByHcPartyAndIdentifiersFilter(
	private val maintenanceTaskDAO: MaintenanceTaskDAO,
	private val sessionLogic: SessionInformationProvider,
) : Filter<String, MaintenanceTask, MaintenanceTaskByHcPartyAndIdentifiersFilter> {
	override fun resolve(
		filter: MaintenanceTaskByHcPartyAndIdentifiersFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	) = flow {
		try {
			emitAll(
				maintenanceTaskDAO.listMaintenanceTaskIdsByHcPartyAndIdentifier(
					datastoreInformation = datastoreInformation,
					searchKeys = sessionLogic.getAllSearchKeysIfCurrentDataOwner(
						requireNotNull(filter.healthcarePartyId ?: sessionLogic.getCurrentDataOwnerIdOrNull()) {
							"A MaintenanceTaskByHcPartyAndIdentifiersFilter must either provide an explicit dataOwnerId or must be used by a data owner user"
						}
					),
					identifiers = filter.identifiers,
				),
			)
		} catch (e: LoginException) {
			throw IllegalArgumentException(e)
		}
	}
}
