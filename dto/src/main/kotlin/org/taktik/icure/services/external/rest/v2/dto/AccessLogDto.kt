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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.base.HasEncryptionMetadataDto
import org.taktik.icure.services.external.rest.v2.dto.base.ICureDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.DelegationDto
import org.taktik.icure.services.external.rest.v2.dto.embed.EncryptableDto
import org.taktik.icure.services.external.rest.v2.dto.embed.SecurityMetadataDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto
import org.taktik.icure.utils.InstantDeserializer
import org.taktik.icure.utils.InstantSerializer
import java.time.Instant

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = """This entity represents Access Log.""")
/**
 * Represents an access log entry that records access to medical data or resources within the system.
 */
data class AccessLogDto(
	/** The Id of the access log. We encourage using either a v4 UUID or a HL7 Id. */
	@param:Schema(description = "The Id of the Access log. We encourage using either a v4 UUID or a HL7 Id") override val id: String,
	@param:Schema(
		description = "The revision of the access log in the database, used for conflict management / optimistic locking.",
	/** The revision of the access log in the database, used for conflict management / optimistic locking. */
	) override val rev: String? = null,
	/** The timestamp (unix epoch in ms) of creation of this entity. */
	override val created: Long? = null,
	/** The timestamp (unix epoch in ms) of the latest modification of this entity. */
	override val modified: Long? = null,
	/** The id of the User that created this access log. */
	override val author: String? = null,
	/** The id of the data owner that is responsible for this access log. */
	override val responsible: String? = null,
	/** The medical location where this entity was created. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	override val medicalLocationId: String? = null,
	/** Tags that qualify the access log as being member of a certain class. */
	override val tags: Set<CodeStubDto> = emptySet(),
	/** Codes that identify or qualify this particular access log. */
	override val codes: Set<CodeStubDto> = emptySet(),
	/** Soft delete (unix epoch in ms) timestamp of the object. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val endOfLife: Long? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	override val deletionDate: Long? = null,
	/** Id of the object that is being requested. */
	@param:Schema(description = "Id of the object that is being requested.") val objectId: String? = null,
	/** The type of access. */
	@param:Schema(description = "The type of access") val accessType: String? = null,
	/** Id of the user making the requests. */
	@param:Schema(description = "Id of the user making the requests") val user: String? = null,
	/** Further details about the access. */
	@param:Schema(description = "Further details about the access") val detail: String? = null,
	@param:JsonSerialize(
		using = InstantSerializer::class,
		include = JsonSerialize.Inclusion.NON_NULL,
	)
	/** The date of logging, filled instantaneously. */
	@param:JsonDeserialize(using = InstantDeserializer::class)
	@param:Schema(description = "The date (unix epoch in ms) of logging, is filled instantaneously.") val date: Instant? = null,
	/** The patient id. Deprecated: use cryptedForeignKeys instead. */
	@Deprecated("Use cryptedForeignKeys instead") val patientId: String? = null,
	/** The secret foreign keys of the access log, used for secure linking to patients. */
	override val secretForeignKeys: Set<String> = emptySet(),
	/** The encrypted foreign keys, mapping owner data owner id to encrypted patient ids. */
	override val cryptedForeignKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	/** The delegations giving access to all connected healthcare information. */
	override val delegations: Map<String, Set<DelegationDto>> = emptyMap(),
	/** The encryption keys used to encrypt the secured properties, encrypted for separate Crypto Actors. */
	override val encryptionKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	/** The base64-encoded encrypted fields of this access log. */
	override val encryptedSelf: Base64StringDto? = null,
	/** The security metadata of this entity, for access control. */
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
