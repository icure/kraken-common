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
import org.taktik.icure.domain.filter.patient.PatientByHcPartyAndActiveFilter
import org.taktik.icure.entities.Patient
import org.taktik.icure.utils.getLoggedHealthCarePartyId
import javax.security.auth.login.LoginException

@Service
@Profile("app")
class PatientByHcPartyAndActiveFilter(
	private val patientDAO: PatientDAO,
	private val sessionLogic: SessionInformationProvider
) : Filter<String, Patient, PatientByHcPartyAndActiveFilter> {

	override fun resolve(
        filter: PatientByHcPartyAndActiveFilter,
        context: Filters,
        datastoreInformation: IDatastoreInformation
    ) = flow {
		try {
			emitAll(patientDAO.listPatientIdsByActive(
				datastoreInformation = datastoreInformation,
				active = filter.active,
				searchKeys = sessionLogic.getAllSearchKeysIfCurrentDataOwner(filter.healthcarePartyId ?: getLoggedHealthCarePartyId(sessionLogic))
			))
		} catch (e: LoginException) {
			throw IllegalArgumentException(e)
		}
	}
}
