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
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.base.HasEncryptionMetadataDto
import org.taktik.icure.services.external.rest.v2.dto.base.HasEndOfLifeDto
import org.taktik.icure.services.external.rest.v2.dto.base.ICureDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.base.VersionableDto
import org.taktik.icure.services.external.rest.v2.dto.embed.DelegationDto
import org.taktik.icure.services.external.rest.v2.dto.embed.SecurityMetadataDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * A lightweight stub representation of an iCure document, containing only the core metadata fields
 * such as identifiers, timestamps, tags, codes, and encryption metadata. Used when the full entity
 * payload is not needed.
 */
data class IcureStubDto(
	/** The unique identifier of the entity. */
	override val id: String,
	/** The revision identifier for optimistic locking. */
	override val rev: String? = null,
	/** The timestamp in epoch milliseconds when the entity was created. */
	override val created: Long? = null,
	/** The timestamp in epoch milliseconds when the entity was last modified. */
	override val modified: Long? = null,
	/** The identifier of the user who created this entity. */
	override val author: String? = null,
	/** The identifier of the data owner responsible for this entity. */
	override val responsible: String? = null,
	/** The identifier of the medical location associated with this entity. Deprecated for use with Cardinal SDK. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	override val medicalLocationId: String? = null,
	/** The set of tags associated with this entity. */
	override val tags: Set<CodeStubDto> = emptySet(),
	/** The set of codes associated with this entity. */
	override val codes: Set<CodeStubDto> = emptySet(),
	/** The soft-delete timestamp in epoch milliseconds. */
	override val endOfLife: Long? = null,
	/** The set of secret foreign keys used for encryption. */
	override val secretForeignKeys: Set<String> = emptySet(),
	/** The map of encrypted foreign keys, keyed by data owner identifier. */
	override val cryptedForeignKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	/** The map of access delegations, keyed by data owner identifier. */
	override val delegations: Map<String, Set<DelegationDto>> = emptyMap(),
	/** The map of encryption keys, keyed by data owner identifier. */
	override val encryptionKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	/** The security metadata containing secure delegation information. */
	override val securityMetadata: SecurityMetadataDto? = null,
) : ICureDocumentDto<String>,
	VersionableDto<String>,
	HasEncryptionMetadataDto,
	HasEndOfLifeDto {
	override fun withIdRev(
		id: String?,
		rev: String,
	) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
}
