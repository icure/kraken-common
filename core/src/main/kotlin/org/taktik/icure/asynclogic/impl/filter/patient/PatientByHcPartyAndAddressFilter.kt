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

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.PatientDAO
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.impl.filter.Filter
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.IDatastoreInformation
import org.taktik.icure.domain.filter.patient.PatientByHcPartyAndAddressFilter
import org.taktik.icure.entities.Patient
import org.taktik.icure.utils.getLoggedHealthCarePartyId
import org.taktik.icure.utils.mergeUniqueIdsForSearchKeys
import javax.security.auth.login.LoginException

@Service
@Profile("app")
class PatientByHcPartyAndAddressFilter(
	private val patientDAO: PatientDAO,
	private val sessionLogic: SessionInformationProvider,
) : Filter<String, Patient, PatientByHcPartyAndAddressFilter> {
	override fun resolve(
		filter: PatientByHcPartyAndAddressFilter,
		context: Filters,
		datastoreInformation: IDatastoreInformation,
	) = flow {
		try {
			val searchKeys = sessionLogic.getAllSearchKeysIfCurrentDataOwner(filter.healthcarePartyId ?: getLoggedHealthCarePartyId(sessionLogic))
			if (filter.postalCode.isNullOrEmpty() && filter.houseNumber.isNullOrEmpty()) {
				mergeUniqueIdsForSearchKeys(searchKeys) { key ->
					patientDAO.listPatientIdsByHcPartyAndAddress(
						datastoreInformation = datastoreInformation,
						searchString = filter.searchString,
						healthcarePartyId = key,
					)
				}.let { emitAll(it) }
			} else {
				mergeUniqueIdsForSearchKeys(searchKeys) { key ->
					patientDAO.listPatientIdsByHcPartyAndAddress(
						datastoreInformation = datastoreInformation,
						streetAndCity = filter.searchString,
						postalCode = filter.postalCode,
						houseNumber = filter.houseNumber,
						healthcarePartyId = key,
					)
				}.let { emitAll(it) }
			}
		} catch (e: LoginException) {
			throw IllegalArgumentException(e)
		}
	}
}
