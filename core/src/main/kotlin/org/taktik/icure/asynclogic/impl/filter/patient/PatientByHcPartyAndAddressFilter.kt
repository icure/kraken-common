/*
 *  iCure Data Stack. Copyright (c) 2020 Taktik SA
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful, but
 *     WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public
 *     License along with this program.  If not, see
 *     <https://www.gnu.org/licenses/>.
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
import org.taktik.icure.domain.filter.patient.PatientByHcPartyAndAddressFilter
import org.taktik.icure.entities.Patient
import org.taktik.icure.utils.getLoggedHealthCarePartyId

@Service
@Profile("app")
class PatientByHcPartyAndAddressFilter(
	private val patientLogic: PatientLogic,
	private val sessionLogic: SessionInformationProvider
) : Filter<String, Patient, PatientByHcPartyAndAddressFilter> {

	override fun resolve(
        filter: PatientByHcPartyAndAddressFilter,
        context: Filters,
        datastoreInformation: IDatastoreInformation?
    ) = flow {
		try {
			if(filter.postalCode.isNullOrEmpty() && filter.houseNumber.isNullOrEmpty()){
				emitAll(patientLogic.listPatientIdsByHcPartyAndAddressOnly(filter.searchString, filter.healthcarePartyId ?: getLoggedHealthCarePartyId(sessionLogic)))
			}else{
				emitAll(patientLogic.listPatientIdsByHcPartyAndAddressOnly(filter.searchString, filter.postalCode, filter.houseNumber, filter.healthcarePartyId ?: getLoggedHealthCarePartyId(sessionLogic)))
			}
		} catch (e: LoginException) {
			throw IllegalArgumentException(e)
		}
	}
}
