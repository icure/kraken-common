/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v1.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v1.dto.base.EncryptableDto
import org.taktik.icure.services.external.rest.v1.dto.base.ICureDocumentDto
import org.taktik.icure.services.external.rest.v1.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v1.dto.embed.DelegationDto
import org.taktik.icure.services.external.rest.v1.dto.embed.SecurityMetadataDto
import org.taktik.icure.services.external.rest.v1.dto.embed.TimeTableItemDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class TimeTableDto(
	override val id: String,
	override val rev: String? = null,
	override val created: Long? = null,
	override val modified: Long? = null,
	override val author: String? = null,
	override val responsible: String? = null,
	override val medicalLocationId: String? = null,
	@JsonInclude(JsonInclude.Include.NON_EMPTY) override val tags: Set<CodeStubDto> = emptySet(),
	@JsonInclude(JsonInclude.Include.NON_EMPTY) override val codes: Set<CodeStubDto> = emptySet(),
	override val endOfLife: Long? = null,
	override val deletionDate: Long? = null,

	val name: String? = null,
	val agendaId: String? = null,
	val startTime: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20150101235960.
	val endTime: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20150101235960.
	@JsonInclude(JsonInclude.Include.NON_EMPTY) val items: List<TimeTableItemDto> = emptyList(),

	@JsonInclude(JsonInclude.Include.NON_EMPTY) override val secretForeignKeys: Set<String> = emptySet(),
	@JsonInclude(JsonInclude.Include.NON_EMPTY) override val cryptedForeignKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	@JsonInclude(JsonInclude.Include.NON_EMPTY) override val delegations: Map<String, Set<DelegationDto>> = emptyMap(),
	@JsonInclude(JsonInclude.Include.NON_EMPTY) override val encryptionKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	override val encryptedSelf: String? = null,
	override val securityMetadata: SecurityMetadataDto?
) : StoredDocumentDto, ICureDocumentDto<String>, EncryptableDto {
	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
