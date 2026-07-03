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
package org.taktik.icure.domain.filter.impl.insurance

import org.taktik.icure.domain.filter.AbstractFilter
import org.taktik.icure.domain.filter.insurance.InsuranceByIdentifiersFilter
import org.taktik.icure.entities.Insurance
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.embed.Identifier

data class InsuranceByIdentifiersFilter(
	override val identifiers: List<Identifier>,
	override val desc: String? = null,
) : AbstractFilter<Insurance>,
	InsuranceByIdentifiersFilter {

	override val canBeUsedInWebsocket = true
	override val requiresSecurityPrecondition: Boolean = false
	override fun requestedDataOwnerIds(): Set<String> = emptySet()

	override fun matches(item: Insurance, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean = item.deletionDate == null && identifiers.any { searchIdentifier -> item.identifier.any { it.system == searchIdentifier.system && it.value == searchIdentifier.value } }
}
