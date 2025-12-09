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
import org.taktik.icure.domain.filter.healthelement.HealthElementByHcPartyIdentifiersFilter
import org.taktik.icure.entities.HealthElement
import javax.security.auth.login.LoginException

@Service
@Profile("app")
class HealthElementByHcPartyIdentifiersFilter(
	private val healthElementDAO: HealthElementDAO,
	private val sessionLogic: SessionInformationProvider,
) : Filter<String, HealthElement, HealthElementByHcPartyIdentifiersFilter> {
	override fun resolve(
		filter: HealthElementByHcPartyIdentifiersFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	) = flow {
		try {
			emitAll(
				healthElementDAO.listHealthElementsIdsByHcPartyAndIdentifiers(
					datastoreInformation = datastoreInformation,
					searchKeys = sessionLogic.getAllSearchKeysIfCurrentDataOwner(filter.hcPartyId),
					identifiers = filter.identifiers,
				),
			)
		} catch (e: LoginException) {
			throw IllegalArgumentException(e)
		}
	}
}
