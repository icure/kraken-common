/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.base.EncryptableDto
import org.taktik.icure.services.external.rest.v2.dto.base.ICureDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.base.IdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.DelegationDto
import org.taktik.icure.services.external.rest.v2.dto.embed.SecurityMetadataDto
import org.taktik.icure.services.external.rest.v2.dto.embed.TaskStatusDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class MaintenanceTaskDto(
    override val id: String,
    override val rev: String? = null,
    val identifier: List<IdentifierDto> = listOf(),
    override val created: Long? = null,
    override val modified: Long? = null,
    override val author: String? = null,
    override val responsible: String? = null,
    override val medicalLocationId: String? = null,
    override val tags: Set<CodeStubDto> = emptySet(),
    override val codes: Set<CodeStubDto> = emptySet(),
    override val endOfLife: Long? = null,
    override val deletionDate: Long? = null,

    val taskType: String? = null,
    val properties: Set<PropertyStubDto> = emptySet(),
    val status: TaskStatusDto = TaskStatusDto.pending,

    override val secretForeignKeys: Set<String> = emptySet(),
    override val cryptedForeignKeys: Map<String, Set<DelegationDto>> = emptyMap(),
    override val delegations: Map<String, Set<DelegationDto>> = emptyMap(),
    override val encryptionKeys: Map<String, Set<DelegationDto>> = emptyMap(),
    override val encryptedSelf: Base64StringDto? = null,
    override val securityMetadata: SecurityMetadataDto? = null
) : StoredDocumentDto, ICureDocumentDto<String>, EncryptableDto {
	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
