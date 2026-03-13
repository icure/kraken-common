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
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.base.HasEncryptionMetadataDto
import org.taktik.icure.services.external.rest.v2.dto.base.HasEndOfLifeDto
import org.taktik.icure.services.external.rest.v2.dto.base.ICureDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.DelegationDto
import org.taktik.icure.services.external.rest.v2.dto.embed.EncryptableDto
import org.taktik.icure.services.external.rest.v2.dto.embed.SecurityMetadataDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto

@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents application-level settings stored as key-value pairs. Settings can be stored in plaintext or encrypted form.
 */
data class ApplicationSettingsDto(
	/** The Id of the application settings. */
	override val id: String,
	/** The revision of the application settings in the database, used for conflict management / optimistic locking. */
	override val rev: String? = null,
	/** The timestamp (unix epoch in ms) of creation of this entity. */
	override val created: Long? = null,
	/** The timestamp (unix epoch in ms) of the latest modification of this entity. */
	override val modified: Long? = null,
	/** The id of the User that created this entity. */
	override val author: String? = null,
	/** The id of the data owner that is responsible for this entity. */
	override val responsible: String? = null,
	/** The medical location where this entity was created. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	override val medicalLocationId: String? = null,
	/** Tags that qualify this entity as being member of a certain class. */
	override val tags: Set<CodeStubDto> = emptySet(),
	/** Codes that identify or qualify this entity. */
	override val codes: Set<CodeStubDto> = emptySet(),
	/** Soft delete (unix epoch in ms) timestamp of the object. */
	override val endOfLife: Long? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	override val deletionDate: Long? = null,
	/** A map of plaintext application settings as key-value pairs. */
	val settings: Map<String, String> = emptyMap(),
	/** A map of encrypted application settings as key-value pairs. */
	val encryptedSettings: Map<String, String> = emptyMap(),
	/** The secret foreign keys, used for secure linking. */
	override val secretForeignKeys: Set<String> = emptySet(),
	/** The encrypted foreign keys. */
	override val cryptedForeignKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	/** The delegations giving access to connected healthcare information. */
	override val delegations: Map<String, Set<DelegationDto>> = emptyMap(),
	/** The encryption keys used to encrypt secured properties, encrypted for separate Crypto Actors. */
	override val encryptionKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	/** The security metadata of this entity, for access control. */
	override val securityMetadata: SecurityMetadataDto? = null,
	/** The base64-encoded encrypted fields of this entity. */
	override val encryptedSelf: Base64StringDto? = null,
) : StoredDocumentDto,
	ICureDocumentDto<String>,
	HasEncryptionMetadataDto,
	EncryptableDto,
	HasEndOfLifeDto {
	override fun withIdRev(
		id: String?,
		rev: String,
	) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
