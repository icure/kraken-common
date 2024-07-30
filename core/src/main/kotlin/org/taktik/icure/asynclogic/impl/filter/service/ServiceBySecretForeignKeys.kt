/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl.filter.service

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.taktik.icure.asyncdao.ContactDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.domain.filter.service.ServiceBySecretForeignKeys
import org.taktik.icure.entities.embed.Service
import org.taktik.icure.utils.getLoggedHealthCarePartyId
import javax.security.auth.login.LoginException

@org.springframework.stereotype.Service
@Profile("app")
class ServiceBySecretForeignKeys(
	private val contactDAO: ContactDAO,
	private val sessionLogic: SessionInformationProvider
) : Filter<String, Service, ServiceBySecretForeignKeys> {

	override fun resolve(
        filter: ServiceBySecretForeignKeys,
        context: Filters,
        datastoreInformation: IDatastoreInformation
    ) = flow {
		try {
			val hcPartyId = filter.healthcarePartyId ?: getLoggedHealthCarePartyId(sessionLogic)
			emitAll(
				contactDAO.listServicesIdsByPatientForeignKeys(
					datastoreInformation = datastoreInformation,
					searchKeys = sessionLogic.getAllSearchKeysIfCurrentDataOwner(hcPartyId),
					patientSecretForeignKeys = filter.patientSecretForeignKeys
				)
			)
		} catch (e: LoginException) {
			throw IllegalArgumentException(e)
		}
	}
}
