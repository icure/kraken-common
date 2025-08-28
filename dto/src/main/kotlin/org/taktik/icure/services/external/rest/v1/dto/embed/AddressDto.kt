/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.embed

/**
 * Created by aduchate on 21/01/13, 14:43
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v1.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v1.dto.base.HasCodesDto
import org.taktik.icure.services.external.rest.v1.dto.base.HasTagsDto
import org.taktik.icure.services.external.rest.v1.dto.base.IdentifierDto
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = """This entity represents an Address""")
data class AddressDto(
	override val tags: Set<CodeStubDto> = emptySet(),
	override val codes: Set<CodeStubDto> = emptySet(),
	@param:Schema(description = "The identifiers of the Address") val identifier: List<IdentifierDto> = emptyList(),
	@param:Schema(description = "The type of place the address represents, ex: home, office, hospital, clinic, etc. ") val addressType: AddressTypeDto? = null,
	@param:Schema(description = "Descriptive notes about the address") val descr: String? = null,
	@param:Schema(description = "Street name") val street: String? = null,
	@param:Schema(description = "Building / house number") val houseNumber: String? = null,
	@param:Schema(description = "Post / PO box number") val postboxNumber: String? = null,
	@param:Schema(description = "Postal/PIN/ZIP/Area code") val postalCode: String? = null,
	@param:Schema(description = "Name of city in the address") val city: String? = null,
	@param:Schema(description = "Name of state in the Address") val state: String? = null,
	@param:Schema(description = "Name / code of country in the address") val country: String? = null,
	@param:Schema(description = "Additional notes", deprecated = true) val note: String? = null,
	@param:Schema(description = "Additional notes") val notes: List<AnnotationDto> = emptyList(),
	@param:Schema(description = "List of other contact details available through telecom services, ex: email, phone number, fax, etc.") val telecoms: List<TelecomDto> = emptyList(),
	override val encryptedSelf: String? = null,
) : EncryptableDto,
	Serializable,
	Comparable<AddressDto>,
	HasTagsDto,
	HasCodesDto {
	override fun compareTo(other: AddressDto): Int = addressType?.compareTo(other.addressType ?: AddressTypeDto.other) ?: 0
}
