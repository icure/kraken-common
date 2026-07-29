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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.dto.annotations.filtering.ActiveField
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.base.HasEncryptionMetadataDto
import org.taktik.icure.services.external.rest.v2.dto.base.HasEndOfLifeDto
import org.taktik.icure.services.external.rest.v2.dto.base.HasIdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.base.ICureDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.base.IdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.base.PersonDto
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.AddressDto
import org.taktik.icure.services.external.rest.v2.dto.embed.DelegationDto
import org.taktik.icure.services.external.rest.v2.dto.embed.EncryptableDto
import org.taktik.icure.services.external.rest.v2.dto.embed.GenderDto
import org.taktik.icure.services.external.rest.v2.dto.embed.PersonNameDto
import org.taktik.icure.services.external.rest.v2.dto.embed.SecurityMetadataDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto

/**
 * A person related to one or more patients, that is neither a patient nor a healthcare party: typically a contact
 * person (parent of a child patient, caregiver, ...), referenced from a patient's partnership with
 * partnerType = relatedPerson. It is a standalone encryptable entity but NOT a crypto actor nor a data owner.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
	description = """A person related to one or more patients, that is neither a patient nor a healthcare party: typically a contact person. It is a standalone encryptable entity but NOT a crypto actor nor a data owner.""",
)
data class RelatedPersonDto(
	/** The Id of the related person. We encourage using either a v4 UUID or a HL7 Id. */
	@param:Schema(description = "The Id of the related person. We encourage using either a v4 UUID or a HL7 Id.") override val id: String,
	/** The revision of the related person in the database, used for conflict management / optimistic locking. */
	@param:Schema(description = "The revision of the related person in the database, used for conflict management / optimistic locking.")
	override val rev: String? = null,
	/** The identifiers of the related person. */
	@ActiveField override val identifier: List<IdentifierDto> = emptyList(),
	/** The timestamp (unix epoch in ms) of creation. */
	override val created: Long? = null,
	/** The timestamp (unix epoch in ms) of the latest modification. */
	override val modified: Long? = null,
	/** The id of the User that created this related person. */
	override val author: String? = null,
	/** The id of the data owner that is responsible for this related person. */
	override val responsible: String? = null,
	/** The id of the medical location where this related person was created. */
	override val medicalLocationId: String? = null,
	/** Tags that qualify the related person as being member of a certain class. */
	override val tags: Set<CodeStubDto> = emptySet(),
	/** Codes that identify or qualify this particular related person. */
	override val codes: Set<CodeStubDto> = emptySet(),
	/** Soft delete (unix epoch in ms) timestamp of the object. */
	override val endOfLife: Long? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	override val deletionDate: Long? = null,

	/** The firstname (name) of the related person. */
	@param:Schema(description = "the firstname (name) of the related person.") override val firstName: String? = null,
	/** The lastname (surname) of the related person. */
	@param:Schema(description = "the lastname (surname) of the related person.") override val lastName: String? = null,
	/** The list of all names of the related person, also containing the official full name information. */
	@param:Schema(description = "the list of all names of the related person, also containing the official full name information.")
	override val names: List<PersonNameDto> = emptyList(),
	/** The name of the company this related person is member of. */
	@param:Schema(description = "the name of the company this related person is member of.") override val companyName: String? = null,
	/** The list of languages spoken by the related person, in ISO 639-2 alpha-2 code. */
	@param:Schema(description = "the list of languages spoken by the related person, in ISO 639-2 alpha-2 code.")
	override val languages: List<String> = emptyList(),
	/** The addresses and telecoms of the related person. */
	@param:Schema(description = "the list of addresses (with address type).") override val addresses: List<AddressDto> = emptyList(),
	/** Mr., Ms., Pr., Dr. ... */
	@param:Schema(description = "Mr., Ms., Pr., Dr. ...") override val civility: String? = null,
	/** The gender of the related person. */
	@param:Schema(description = "the gender of the related person") override val gender: GenderDto? = GenderDto.unknown,
	/** Extra properties of the related person. */
	@param:Schema(description = "Extra properties") @ActiveField val properties: Set<PropertyStubDto> = emptySet(),

	/** The secret foreign keys of this entity. */
	override val secretForeignKeys: Set<String> = emptySet(),
	/** The foreign keys encrypted in the delegates' AES keys. */
	override val cryptedForeignKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	/** The delegations giving access to this related person. */
	override val delegations: Map<String, Set<DelegationDto>> = emptyMap(),
	/** The keys used to encrypt this entity when the entity is stored encrypted. */
	override val encryptionKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	/** The base64-encoded encrypted fields of this entity. */
	override val encryptedSelf: Base64StringDto? = null,
	/** The security metadata of the entity. */
	override val securityMetadata: SecurityMetadataDto? = null,
) : StoredDocumentDto,
	ICureDocumentDto<String>,
	PersonDto,
	HasEncryptionMetadataDto,
	EncryptableDto,
	HasIdentifierDto,
	HasEndOfLifeDto {
	override fun withIdRev(
		id: String?,
		rev: String,
	) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
