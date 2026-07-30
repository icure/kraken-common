package org.taktik.icure.services.external.rest.v2.dto

import com.icure.cardinal.entities.RawJson
import org.taktik.icure.dto.annotations.filtering.ActiveField
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.base.CustomisableRootDto
import org.taktik.icure.services.external.rest.v2.dto.base.ExtendableDto
import org.taktik.icure.services.external.rest.v2.dto.base.HasEncryptionMetadataDto
import org.taktik.icure.services.external.rest.v2.dto.base.ICureDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.DataAttachmentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.DelegationDto
import org.taktik.icure.services.external.rest.v2.dto.embed.DeletedAttachmentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.SecurityMetadataDto

data class CustomEntityBaseDto(
	override val id: String,
	override val rev: String? = null,
	override val deletionDate: Long? = null,
	override val secretForeignKeys: Set<String> = emptySet(),
	override val cryptedForeignKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	override val delegations: Map<String, Set<DelegationDto>> = emptyMap(),
	override val encryptionKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	override val securityMetadata: SecurityMetadataDto? = null,
	override val created: Long? = null,
	override val modified: Long? = null,
	override val author: String? = null,
	override val responsible: String? = null,
	@ActiveField val endOfLife: Long? = null,
	override val tags: Set<CodeStubDto> = emptySet(),
	override val codes: Set<CodeStubDto> = emptySet(),
	override val customisedModelVersion: Int? = null,
	override val extensions: RawJson.JsonObject? = null,
	@ActiveField val dataAttachments: Map<String, DataAttachmentDto> = emptyMap(),
	@ActiveField val deletedAttachments: List<DeletedAttachmentDto> = emptyList(),
) : HasEncryptionMetadataDto,
	StoredDocumentDto,
	ICureDocumentDto<String>,
	CustomisableRootDto,
	ExtendableDto {
	override fun withIdRev(
		id: String?,
		rev: String,
	): CustomEntityBaseDto = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?): CustomEntityBaseDto = this.copy(deletionDate = deletionDate)
}