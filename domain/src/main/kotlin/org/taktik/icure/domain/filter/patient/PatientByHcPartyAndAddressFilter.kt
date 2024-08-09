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
package org.taktik.icure.domain.filter.patient

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.embed.Address

/**
 * Retrieves all the [Patient]s with a delegation for [healthcarePartyId] that match one of the two conditions:
 * - If [postalCode] and [houseNumber] are both not null, [Patient] must have at least an address in [Patient.addresses]
 *   where a concatenation of [Address.street] + [Address.city] starts with [searchString], where [Address.postalCode]
 *   starts with [postalCode], and where [Address.houseNumber] starts with [houseNumber].
 *
 * - Otherwise, [Patient] must have at least an address in [Patient.addresses] that has a concatenation of
 *   [Address.street] + [Address.postalCode] + [Address.city] that starts with the specified [searchString].
 *
 * As this filter explicitly specifies a data owner id, it does not require any security precondition to be used.
 */
interface PatientByHcPartyAndAddressFilter : Filter<String, Patient> {
	val searchString: String?
	val healthcarePartyId: String?
	val postalCode: String?
	val houseNumber: String?
}
