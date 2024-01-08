package org.taktik.icure.asynclogic.impl.filter.service

import javax.security.auth.login.LoginException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.ContactLogic
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.domain.filter.service.ServiceByHcPartyHealthElementIdsFilter
import org.taktik.icure.entities.embed.Service
import org.taktik.icure.utils.getLoggedHealthCarePartyId

@org.springframework.stereotype.Service
@Profile("app")
class ServiceByHcPartyHealthElementIdsFilter(
	private val contactLogic: ContactLogic,
	private val sessionLogic: SessionInformationProvider
) : Filter<String, Service, ServiceByHcPartyHealthElementIdsFilter> {

	override fun resolve(
        filter: ServiceByHcPartyHealthElementIdsFilter,
        context: Filters,
        datastoreInformation: IDatastoreInformation?
    ): Flow<String> = flow {
		try {
			val hcPartyId = filter.healthcarePartyId ?: getLoggedHealthCarePartyId(sessionLogic)
			emitAll(contactLogic.listServiceIdsByHcPartyAndHealthElementIds(hcPartyId, filter.healthElementIds))
		} catch (e: LoginException) {
			throw IllegalArgumentException(e)
		}
	}
}
