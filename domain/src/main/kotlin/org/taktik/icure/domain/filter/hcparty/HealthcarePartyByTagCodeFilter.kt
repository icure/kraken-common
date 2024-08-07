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
package org.taktik.icure.domain.filter.hcparty

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.entities.HealthcareParty

/**
 * Retrieves all the [HealthcareParty]s that either have a code stub in [HealthcareParty.tags] with type [tagType] and code [tagCode] and/or
 * a stub with type [codeType] and code [codeCode] in [HealthcareParty.tags].
 * This filter requires a special permission to be used.
 */
interface HealthcarePartyByTagCodeFilter : Filter<String, HealthcareParty> {
	val tagType: String?
	val tagCode: String?
	val codeType: String?
	val codeCode: String?
}
