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
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.patient.PatientByHcPartyFilter
import org.taktik.icure.entities.Patient
import org.taktik.icure.utils.mergeUniqueIdsForSearchKeys
import javax.security.auth.login.LoginException

@Service
@Profile("app")
class PatientByHcPartyFilter(
	private val patientDAO: PatientDAO,
	private val sessionLogic: SessionInformationProvider,
) : Filter<String, Patient, PatientByHcPartyFilter> {
	override fun resolve(
		filter: PatientByHcPartyFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	) = flow {
		try {
			val hcpId = requireNotNull(filter.healthcarePartyId ?: sessionLogic.getCurrentDataOwnerIdOrNull()) {
				"A PatientByHcPartyFilter must either provide an explicit dataOwnerId or must be used by a data owner user"
			}
			emitAll(
				mergeUniqueIdsForSearchKeys(sessionLogic.getAllSearchKeysIfCurrentDataOwner(hcpId)) { key ->
					patientDAO.listPatientIdsByHcParty(datastoreInformation, key)
				},
			)
		} catch (e: LoginException) {
			throw IllegalArgumentException(e)
		}
	}
}
