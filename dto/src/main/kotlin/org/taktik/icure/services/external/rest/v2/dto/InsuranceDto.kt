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
package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonFilter
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.taktik.icure.dto.annotations.filtering.ActiveField
import org.taktik.icure.dto.annotations.filtering.LegacyField
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.base.HasCodesDto
import org.taktik.icure.services.external.rest.v2.dto.base.HasTagsDto
import org.taktik.icure.services.external.rest.v2.dto.base.IdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.AddressDto

/**
 * Represents an insurance entity. An insurance can be a mutual fund, a private insurance company,
 * or any other type of insurance organization that covers healthcare costs.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.InsuranceDto")
data class InsuranceDto(
	/** The unique identifier of the insurance. */
	override val id: String,
	/** The revision of the insurance in the database, used for conflict management / optimistic locking. */
	override val rev: String? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	override val deletionDate: Long? = null,
	/** The name of the insurance in different languages. */
	@ActiveField val name: Map<String, String> = emptyMap(),
	/** The identifiers of the insurance. */
	@ActiveField
	val identifiers: List<IdentifierDto> = listOf(),
	/** Tags that qualify the insurance as being member of a certain class. */
	@ActiveField
	override val tags: Set<CodeStubDto> = emptySet(),
	/** Codes that identify or qualify this particular insurance. */
	@ActiveField
	override val codes: Set<CodeStubDto> = emptySet(),
	/** Whether this is a private insurance. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@LegacyField
	val privateInsurance: Boolean = false,
	/** Whether this insurance covers hospitalisation. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@LegacyField
	val hospitalisationInsurance: Boolean = false,
	/** Whether this insurance covers ambulatory care. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@LegacyField
	val ambulatoryInsurance: Boolean = false,
	/** The insurance code. */
	@ActiveField val code: String? = null,
	/** The agreement number for the insurance. */
	@ActiveField val agreementNumber: String? = null,
	/** The id of the parent insurance entity. */
	@ActiveField val parent: String? = null, // ID of the parent
	/** The address of the insurance company. */
	@ActiveField val address: AddressDto = AddressDto(),
) : StoredDocumentDto, HasTagsDto, HasCodesDto {
	override fun withIdRev(
		id: String?,
		rev: String,
	) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
