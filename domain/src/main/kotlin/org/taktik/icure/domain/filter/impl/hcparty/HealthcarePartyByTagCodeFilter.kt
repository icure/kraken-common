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
package org.taktik.icure.domain.filter.impl.hcparty

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.base.containsStubWithTypeAndCode

data class HealthcarePartyByTagCodeFilter(
	override val desc: String? = null,
	override val tagType: String? = null,
	override val tagCode: String? = null,
	override val codeType: String? = null,
	override val codeCode: String? = null
) : AbstractFilter<HealthcareParty>, org.taktik.icure.domain.filter.hcparty.HealthcarePartyByTagCodeFilter {
	init {
		if (tagCode != null) {
			require(tagType != null) { "If you specify tagCode you must also specify tagType" }
		}
		if (codeCode != null) {
			require(codeType != null) { "If you specify codeCode you must also specify codeType" }
		}
		require(tagType != null || codeType != null) { "You must specify at least one of tagType or codeType" }
	}

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = true
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: HealthcareParty, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean {
		return (tagType == null || item.tags.containsStubWithTypeAndCode(tagType, tagCode)) &&
			(codeType == null || item.codes.containsStubWithTypeAndCode(codeType, codeCode))
	}
}
