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
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.domain.filter.healthelement.HealthElementByDataOwnerPatientOpeningDate
import org.taktik.icure.entities.HealthElement
import org.taktik.icure.utils.getLoggedHealthCarePartyId
import javax.security.auth.login.LoginException

@Service
@Profile("app")
class HealthElementByDataOwnerPatientOpeningDate(
	private val healthElementDAO: HealthElementDAO,
	private val sessionLogic: SessionInformationProvider
) : Filter<String, HealthElement, HealthElementByDataOwnerPatientOpeningDate> {
	override fun resolve(
        filter: HealthElementByDataOwnerPatientOpeningDate,
        context: Filters,
        datastoreInformation: IDatastoreInformation
    ) = flow {
		try {
			val hcPartyId = filter.healthcarePartyId ?: getLoggedHealthCarePartyId(sessionLogic)
			emitAll(healthElementDAO.listHealthElementIdsByDataOwnerPatientOpeningDate(
				datastoreInformation = datastoreInformation,
				searchKeys = sessionLogic.getAllSearchKeysIfCurrentDataOwner(hcPartyId),
				secretForeignKeys = filter.patientSecretForeignKeys,
				startDate = filter.startDate,
				endDate = filter.endDate,
				descending = filter.descending
			))
		} catch (e: LoginException) {
			throw IllegalArgumentException(e)
		}
	}
}
