/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.asynclogic.impl.filter.patient

import javax.security.auth.login.LoginException
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.PatientLogic
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.domain.filter.patient.PatientByHcPartyAndSsinsFilter
import org.taktik.icure.entities.Patient
import org.taktik.icure.utils.getLoggedHealthCarePartyId

@Service
@Profile("app")
class PatientByHcPartyAndSsinsFilter(
	private val patientLogic: PatientLogic,
	private val sessionLogic: SessionInformationProvider
) : Filter<String, Patient, PatientByHcPartyAndSsinsFilter> {

	override fun resolve(
        filter: PatientByHcPartyAndSsinsFilter,
        context: Filters,
        datastoreInformation: IDatastoreInformation?
    ) = flow {
		try {
			emitAll(patientLogic.listByHcPartyAndSsinsIdsOnly(filter.ssins ?: listOf(), filter.healthcarePartyId ?: getLoggedHealthCarePartyId(sessionLogic)))
		} catch (e: LoginException) {
			throw IllegalArgumentException(e)
		}
	}
}
