package org.taktik.icure.asynclogic.impl.filter.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.taktik.icure.asyncdao.ContactDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.service.ServiceByHcPartyHealthElementIdsFilter
import org.taktik.icure.entities.embed.Service
import javax.security.auth.login.LoginException

@org.springframework.stereotype.Service
@Profile("app")
class ServiceByHcPartyHealthElementIdsFilter(
	private val contactDAO: ContactDAO,
	private val sessionLogic: SessionInformationProvider,
) : Filter<String, Service, ServiceByHcPartyHealthElementIdsFilter> {
	override fun resolve(
		filter: ServiceByHcPartyHealthElementIdsFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	): Flow<String> = flow {
		try {
			val hcPartyId = requireNotNull(filter.healthcarePartyId ?: sessionLogic.getCurrentDataOwnerIdOrNull()) {
				"A ServiceByHcPartyHealthElementIdsFilter must either provide an explicit dataOwnerId or must be used by a data owner user"
			}
			emitAll(
				contactDAO.listServiceIdsByHcPartyHealthElementIds(
					datastoreInformation = datastoreInformation,
					searchKeys = sessionLogic.getAllSearchKeysIfCurrentDataOwner(hcPartyId),
					healthElementIds = filter.healthElementIds,
				),
			)
		} catch (e: LoginException) {
			throw IllegalArgumentException(e)
		}
	}
}
