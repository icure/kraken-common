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
package org.taktik.icure.services.external.rest.v2.dto.base

import org.taktik.icure.services.external.rest.v2.dto.embed.AddressDto
import org.taktik.icure.services.external.rest.v2.dto.embed.GenderDto
import org.taktik.icure.services.external.rest.v2.dto.embed.PersonNameDto
import java.io.Serializable
import org.taktik.icure.dto.annotations.filtering.ActiveField

/**
 * Interface for entities that represent a person with personal details and contact information.
 */
interface PersonDto :
	Serializable,
	IdentifiableDto<String> {
	@ActiveField val civility: String?
	@ActiveField val gender: GenderDto?
	@ActiveField val firstName: String?
	@ActiveField val lastName: String?
	@ActiveField val companyName: String?
	@ActiveField val names: List<PersonNameDto>
	@ActiveField val addresses: List<AddressDto>
	@ActiveField val languages: List<String>
}
