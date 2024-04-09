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
package org.taktik.icure.domain.filter.impl.patient

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.Patient
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class PatientByHcPartyAndAddressFilter(
	override val desc: String? = null,
	override val searchString: String? = null,
	override val healthcarePartyId: String? = null,
	override val postalCode: String? = null,
	override val houseNumber: String? = null
) : AbstractFilter<Patient>, org.taktik.icure.domain.filter.patient.PatientByHcPartyAndAddressFilter {

	// The HCP id is coalesced in the resolve
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = healthcarePartyId?.let { setOf(it) } ?: emptySet()

	override fun matches(item: Patient, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean {
		return (healthcarePartyId == null || searchKeyMatcher(healthcarePartyId, item))
				&& (searchString == null || item.addresses.any {adr -> adr.street?.contains(searchString)!! } ) //TODO: implement filter
		//||  item.addresses.any {adr -> adr.telecoms?.any { tc -> tc.telecomNumber?.contains(searchString)!! } }
	}
}
