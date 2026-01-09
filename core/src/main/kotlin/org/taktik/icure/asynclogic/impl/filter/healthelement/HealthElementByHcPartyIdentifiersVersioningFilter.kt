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
import org.taktik.icure.domain.filter.VersionFiltering
import org.taktik.icure.domain.filter.healthelement.HealthElementByHcPartyIdentifiersVersioningFilter
import org.taktik.icure.entities.HealthElement
import javax.security.auth.login.LoginException

@Service
@Profile("app")
class HealthElementByHcPartyIdentifiersVersioningFilter(
	private val healthElementDAO: HealthElementDAO,
	private val sessionLogic: SessionInformationProvider,
) : Filter<String, HealthElement, HealthElementByHcPartyIdentifiersVersioningFilter> {
	override fun resolve(
		filter: HealthElementByHcPartyIdentifiersVersioningFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	) = flow {
		try {
			emitAll(
				healthElementDAO.listHealthElementsIdsByHcPartyAndIdentifiersAndVersioning(
					datastoreInformation = datastoreInformation,
					searchKeys = sessionLogic.getAllSearchKeysIfCurrentDataOwner(filter.hcPartyId),
					identifiers = filter.identifiers,
					filterVersion = filter.versionFiltering ?: VersionFiltering.ANY,
				),
			)
		} catch (e: LoginException) {
			throw IllegalArgumentException(e)
		}
	}
}

