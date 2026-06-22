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
package org.taktik.icure.services.external.rest.v2.dto.embed

/**
 * Created by aduchate on 21/01/13, 14:43
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.base.HasCodesDto
import org.taktik.icure.services.external.rest.v2.dto.base.HasTagsDto
import org.taktik.icure.services.external.rest.v2.dto.base.IdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto
import java.io.Serializable
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = """This entity represents an Address""")
data class AddressDto(
	override val tags: Set<CodeStubDto> = emptySet(),
	override val codes: Set<CodeStubDto> = emptySet(),
	@param:Schema(description = "The identifiers of the Address") @ActiveField val identifier: List<IdentifierDto> = emptyList(),
	@param:Schema(description = "The type of place the address represents, ex: home, office, hospital, clinic, etc. ") @ActiveField val addressType: AddressTypeDto? = null,
	@param:Schema(description = "Descriptive notes about the address") @ActiveField val descr: String? = null,
	@param:Schema(description = "Street name") @ActiveField val street: String? = null,
	@param:Schema(description = "Building / house number") @ActiveField val houseNumber: String? = null,
	@param:Schema(description = "Post / PO box number") @ActiveField val postboxNumber: String? = null,
	@param:Schema(description = "Postal/PIN/ZIP/Area code") @ActiveField val postalCode: String? = null,
	@param:Schema(description = "Name of city in the address") @ActiveField val city: String? = null,
	@param:Schema(description = "Name of state in the Address") @ActiveField val state: String? = null,
	@param:Schema(description = "Name / code of country in the address") @ActiveField val country: String? = null,
	@param:Schema(description = "Additional notes", deprecated = true) @ActiveField val note: String? = null,
	@param:Schema(description = "Additional notes") @ActiveField val notes: List<AnnotationDto> = emptyList(),
	@param:Schema(description = "List of other contact details available through telecom services, ex: email, phone number, fax, etc.") @ActiveField val telecoms: List<TelecomDto> = emptyList(),
	override val encryptedSelf: Base64StringDto? = null,
) : EncryptableDto,
	Serializable,
	Comparable<AddressDto>,
	HasTagsDto,
	HasCodesDto {
	override fun compareTo(other: AddressDto): Int = addressType?.compareTo(other.addressType ?: AddressTypeDto.other) ?: 0
}
