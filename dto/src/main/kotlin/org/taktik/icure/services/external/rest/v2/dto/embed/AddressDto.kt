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

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = """This entity represents an Address""")
data class AddressDto(
	override val tags: Set<CodeStubDto> = emptySet(),
	override val codes: Set<CodeStubDto> = emptySet(),
	@get:Schema(description = "The identifiers of the Address") val identifier: List<IdentifierDto> = emptyList(),
	@get:Schema(description = "The type of place the address represents, ex: home, office, hospital, clinic, etc. ") val addressType: AddressTypeDto? = null,
	@get:Schema(description = "Descriptive notes about the address") val descr: String? = null,
	@get:Schema(description = "Street name") val street: String? = null,
	@get:Schema(description = "Building / house number") val houseNumber: String? = null,
	@get:Schema(description = "Post / PO box number") val postboxNumber: String? = null,
	@get:Schema(description = "Postal/PIN/ZIP/Area code") val postalCode: String? = null,
	@get:Schema(description = "Name of city in the address") val city: String? = null,
	@get:Schema(description = "Name of state in the Address") val state: String? = null,
	@get:Schema(description = "Name / code of country in the address") val country: String? = null,
	@get:Schema(description = "Additional notes", deprecated = true) val note: String? = null,
	@get:Schema(description = "Additional notes") val notes: List<AnnotationDto> = emptyList(),
	@get:Schema(description = "List of other contact details available through telecom services, ex: email, phone number, fax, etc.") val telecoms: List<TelecomDto> = emptyList(),
	override val encryptedSelf: Base64StringDto? = null,
) : EncryptableDto,
	Serializable,
	Comparable<AddressDto>,
	HasTagsDto,
	HasCodesDto {
	override fun compareTo(other: AddressDto): Int = addressType?.compareTo(other.addressType ?: AddressTypeDto.other) ?: 0
}
