/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl.filter.patient

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.PatientDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.domain.filter.patient.PatientByHcPartyAndExternalIdFilter
import org.taktik.icure.entities.Patient
import org.taktik.icure.utils.getLoggedHealthCarePartyId
import org.taktik.icure.utils.mergeUniqueIdsForSearchKeys
import javax.security.auth.login.LoginException

@Service
@Profile("app")
class PatientByHcPartyAndExternalIdFilter(
	private val patientDAO: PatientDAO,
	private val sessionLogic: SessionInformationProvider
) : Filter<String, Patient, PatientByHcPartyAndExternalIdFilter> {

	override fun resolve(
        filter: PatientByHcPartyAndExternalIdFilter,
        context: Filters,
        datastoreInformation: IDatastoreInformation
    ) = flow {
		try {
			val hcpId = filter.healthcarePartyId ?: getLoggedHealthCarePartyId(sessionLogic)
			mergeUniqueIdsForSearchKeys(sessionLogic.getAllSearchKeysIfCurrentDataOwner(hcpId)) { key ->
				patientDAO.listPatientIdsByHcPartyAndExternalId(
					datastoreInformation = datastoreInformation,
					externalId = filter.externalId,
					healthcarePartyId = key
				)
			}.let { emitAll(it) }
		} catch (e: LoginException) {
			throw IllegalArgumentException(e)
		}
	}
}
