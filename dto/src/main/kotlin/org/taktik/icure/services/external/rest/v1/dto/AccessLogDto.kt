/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto

import java.time.Instant
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v1.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v1.dto.base.EncryptableDto
import org.taktik.icure.services.external.rest.v1.dto.base.ICureDocumentDto
import org.taktik.icure.services.external.rest.v1.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v1.dto.embed.DelegationDto
import org.taktik.icure.services.external.rest.v1.dto.embed.SecurityMetadataDto
import org.taktik.icure.utils.InstantDeserializer
import org.taktik.icure.utils.InstantSerializer

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = """This entity represents Access Log.""")
data class AccessLogDto(
	@Schema(description = "The Id of the Access log. We encourage using either a v4 UUID or a HL7 Id") override val id: String,
	@Schema(description = "The revision of the access log in the database, used for conflict management / optimistic locking.") override val rev: String? = null,
	override val created: Long? = null,
	override val modified: Long? = null,
	override val author: String? = null,
	override val responsible: String? = null,
	override val medicalLocationId: String? = null,
	override val tags: Set<CodeStubDto> = emptySet(),
	override val codes: Set<CodeStubDto> = emptySet(),
	override val endOfLife: Long?,
	override val deletionDate: Long? = null,
	@Schema(description = "Id of the object that is being requested.") val objectId: String? = null,
	@Schema(description = "The type of access") val accessType: String? = null,
	@Schema(description = "Id of the user making the requests") val user: String? = null,
	@Schema(description = "Further details about the access") val detail: String? = null,
	@JsonSerialize(using = InstantSerializer::class, include = JsonSerialize.Inclusion.NON_NULL) @JsonDeserialize(using = InstantDeserializer::class)
	@Schema(description = "The date (unix epoch in ms) of logging, is filled instantaneously.") val date: Instant? = null,
	@get:Deprecated("Use cryptedForeignKeys instead") val patientId: String? = null,
	override val secretForeignKeys: Set<String> = emptySet(),
	override val cryptedForeignKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	override val delegations: Map<String, Set<DelegationDto>> = emptyMap(),
	override val encryptionKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	override val encryptedSelf: String? = null,
	override val securityMetadata: SecurityMetadataDto? = null
) : StoredDocumentDto, ICureDocumentDto<String>, EncryptableDto {
	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
