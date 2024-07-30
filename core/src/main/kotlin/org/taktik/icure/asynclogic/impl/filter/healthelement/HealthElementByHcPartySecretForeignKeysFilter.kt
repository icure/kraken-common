/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl.filter.healthelement

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.HealthElementDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.domain.filter.healthelement.HealthElementByHcPartySecretForeignKeysFilter
import org.taktik.icure.entities.HealthElement
import org.taktik.icure.utils.getLoggedHealthCarePartyId
import javax.security.auth.login.LoginException

@Service
@Profile("app")
class HealthElementByHcPartySecretForeignKeysFilter(
	private val healthElementDAO: HealthElementDAO,
	private val sessionLogic: SessionInformationProvider
) : Filter<String, HealthElement, HealthElementByHcPartySecretForeignKeysFilter> {
	override fun resolve(
        filter: HealthElementByHcPartySecretForeignKeysFilter,
        context: Filters,
        datastoreInformation: IDatastoreInformation
    ) = flow {
		try {
			val hcPartyId = filter.healthcarePartyId ?: getLoggedHealthCarePartyId(sessionLogic)
			emitAll(healthElementDAO.listHealthElementIdsByHcPartyAndSecretPatientKeys(
				datastoreInformation = datastoreInformation,
				searchKeys = sessionLogic.getAllSearchKeysIfCurrentDataOwner(hcPartyId),
				secretPatientKeys = filter.patientSecretForeignKeys.toList()
			))
		} catch (e: LoginException) {
			throw IllegalArgumentException(e)
		}
	}
}
