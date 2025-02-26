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
import org.taktik.icure.asynclogic.datastore.IDatastoreInformation
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.domain.filter.patient.PatientByDataOwnerTagFilter
import org.taktik.icure.entities.Patient
import javax.security.auth.login.LoginException

@Service
@Profile("app")
class PatientByDataOwnerTagFilter(
	private val patientDAO: PatientDAO,
	private val sessionLogic: SessionInformationProvider
) : Filter<String, Patient, PatientByDataOwnerTagFilter> {

	override fun resolve(
        filter: PatientByDataOwnerTagFilter,
        context: Filters,
        datastoreInformation: IDatastoreInformation
    ) = flow {
		try {
			emitAll(patientDAO.listPatientIdsByDataOwnerTag(
				datastoreInformation = datastoreInformation,
				searchKeys = sessionLogic.getAllSearchKeysIfCurrentDataOwner(filter.dataOwnerId),
				tagType = filter.tagType,
				tagCode = filter.tagCode,
			))
		} catch (e: LoginException) {
			throw IllegalArgumentException(e)
		}
	}
}
