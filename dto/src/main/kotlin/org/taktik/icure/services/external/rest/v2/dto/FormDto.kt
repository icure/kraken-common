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
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.base.HasEncryptionMetadataDto
import org.taktik.icure.services.external.rest.v2.dto.base.ICureDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.DelegationDto
import org.taktik.icure.services.external.rest.v2.dto.embed.EncryptableDto
import org.taktik.icure.services.external.rest.v2.dto.embed.SecurityMetadataDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto

@JsonIgnoreProperties(ignoreUnknown = true)
data class FormDto(
	/** The Id of the form. We encourage using either a v4 UUID or a HL7 Id. */
	@param:Schema(description = "the Id of the form. We encourage using either a v4 UUID or a HL7 Id.") override val id: String,
	/** The revision of the form in the database, used for conflict management / optimistic locking. */
	@param:Schema(description = "the revision of the form in the database, used for conflict management / optimistic locking.") override val rev: String? = null,
	/** The timestamp (unix epoch in ms) of creation of the form, will be filled automatically if missing. Not enforced by the application server. */
	override val created: Long? = null,
	/** The date (unix epoch in ms) of the latest modification of the form, will be filled automatically if missing. Not enforced by the application server. */
	override val modified: Long? = null,
	/** The id of the User that has created this form, will be filled automatically if missing. Not enforced by the application server. */
	override val author: String? = null,
	/** The id of the healthcare party that is responsible for this form, will be filled automatically if missing. Not enforced by the application server. */
	override val responsible: String? = null,
	/** The id of the medical location where the form was created. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	override val medicalLocationId: String? = null,
	/** Tags that qualify the form as being member of a certain class. */
	override val tags: Set<CodeStubDto> = emptySet(),
	/** Codes that identify or qualify this particular form. */
	override val codes: Set<CodeStubDto> = emptySet(),
	/** Soft delete (unix epoch in ms) timestamp of the object. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val endOfLife: Long? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	override val deletionDate: Long? = null,
	val openingDate: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20150101235960.
	val status: String? = null,
	val version: Int? = null,
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val logicalUuid: String? = null,
	/** Name/basic description of the form */
	@param:Schema(description = "Name/basic description of the form") val descr: String? = null,
	/** A unique external id (from another external source). */
	@param:Schema(description = "A unique external id (from another external source).") val uniqueId: String? = null,
	/** Id of the form template being used to display the form */
	@param:Schema(description = "Id of the form template being used to display the form") val formTemplateId: String? = null,
	/** Id of the contact for which the form is being used. */
	@param:Schema(description = "Id of the contact for which the form is being used.") val contactId: String? = null,
	/** The healthcare element to which this form is attached. */
	@param:Schema(description = "The healthcare element to which this form is attached.") val healthElementId: String? = null,
	/** The healthcare approach to which this form is attached. */
	@param:Schema(description = "The healthcare approach to which this form is attached.") val planOfActionId: String? = null,
	/** The parent of this form, used to determine the forms hierarchy */
	@param:Schema(description = "The parent of this form, used to determine the forms hierarchy") val parent: String? = null,
	/** Id of the anchor inside a parent form. When a form can have several series of sub-forms, the anchor is used to identify the series. */
	@param:Schema(
		description = "Id of the anchor inside a parent form. When a form can have several series of sub-forms, the anchor is used to identify the series.",
	) val anchorId: String? = null,
	override val secretForeignKeys: Set<String> = emptySet(),
	override val cryptedForeignKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	/** The delegations giving access to all connected healthcare information. */
	override val delegations: Map<String, Set<DelegationDto>> = emptyMap(),
	/** The patient secret encryption key used to encrypt the secured properties (like note for example), encrypted for separate Crypto Actors. */
	override val encryptionKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	/** The encrypted fields of this Form. */
	override val encryptedSelf: Base64StringDto? = null,
	override val securityMetadata: SecurityMetadataDto? = null,
) : StoredDocumentDto,
	ICureDocumentDto<String>,
	HasEncryptionMetadataDto,
	EncryptableDto {
	override fun withIdRev(
		id: String?,
		rev: String,
	) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
