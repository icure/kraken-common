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
import org.taktik.icure.domain.filter.VersionFiltering
import org.taktik.icure.entities.HealthElement
import org.taktik.icure.entities.embed.Identifier

/**
 * Retrieves all the [HealthElement]s that the data owner with id [hcPartyId] can access and [HealthElement.identifiers]
 * intersects the provided [identifiers].
 * This filter explicitly requires a [hcPartyId], so it does not require any security precondition.
 * The optional [versionFiltering] specifies if matching health elements must be returned only if they are a specific version.
 */
interface HealthElementByHcPartyIdentifiersVersioningFilter : Filter<String, HealthElement> {
	val hcPartyId: String
	val identifiers: List<Identifier>
	val versionFiltering: VersionFiltering?
}
