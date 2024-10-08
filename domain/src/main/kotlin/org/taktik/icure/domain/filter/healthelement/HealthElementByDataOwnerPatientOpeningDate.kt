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

package org.taktik.icure.domain.filter.healthelement

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.HealthElement

/**
 * Retrieves all the [HealthElement]s with a delegation for [healthcarePartyId], where [HealthElement.secretForeignKeys]
 * contains at least one of [patientSecretForeignKeys].
 * If [healthcarePartyId] is null, then the healthcare party id of the user making the request will be used.
 * This filter explicitly requires a [healthcarePartyId], so it does not require any security precondition.
 */
interface HealthElementByDataOwnerPatientOpeningDate : Filter<String, HealthElement> {
	val healthcarePartyId: String?
	val patientSecretForeignKeys: Set<String>
	val startDate: Long?
	val endDate: Long?
	val descending: Boolean
}
