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

import org.taktik.icure.db.sanitize
import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.entities.HealthcareParty
import org.taktik.icure.entities.base.HasEncryptionMetadata

data class HealthcarePartyByNameFilter(
	override val desc: String? = null,
	override val name: String,
	override val descending: Boolean? = null,
) : AbstractFilter<HealthcareParty>, org.taktik.icure.domain.filter.hcparty.HealthcarePartyByNameFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: HealthcareParty, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean {
		return ((item.lastName ?: "") + (item.firstName ?: "")).contains(name.sanitize()!!, true)
	}
}
