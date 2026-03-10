/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v2.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.base.HasEncryptionMetadataDto
import org.taktik.icure.services.external.rest.v2.dto.base.HasEndOfLifeDto
import org.taktik.icure.services.external.rest.v2.dto.base.ICureDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.base.IdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.DelegationDto
import org.taktik.icure.services.external.rest.v2.dto.embed.EncryptableDto
import org.taktik.icure.services.external.rest.v2.dto.embed.SecurityMetadataDto
import org.taktik.icure.services.external.rest.v2.dto.embed.TaskStatusDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents a maintenance task in the iCure system. Maintenance tasks are used to track background operations
 * such as key exchange requests, data migrations, or other administrative operations that require asynchronous processing.
 */
data class MaintenanceTaskDto(
	/** The unique identifier of the maintenance task. */
	override val id: String,
	/** The revision of the maintenance task in the database, used for conflict management / optimistic locking. */
	override val rev: String? = null,
	/** The identifiers of the maintenance task. */
	val identifier: List<IdentifierDto> = listOf(),
	/** The timestamp (unix epoch in ms) of creation. */
	override val created: Long? = null,
	/** The timestamp (unix epoch in ms) of the latest modification. */
	override val modified: Long? = null,
	/** The id of the User that created this maintenance task. */
	override val author: String? = null,
	/** The id of the HealthcareParty that is responsible for this maintenance task. */
	override val responsible: String? = null,
	/** The id of the medical location where this maintenance task was created. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	override val medicalLocationId: String? = null,
	/** Tags that qualify the maintenance task as being member of a certain class. */
	override val tags: Set<CodeStubDto> = emptySet(),
	/** Codes that identify or qualify this particular maintenance task. */
	override val codes: Set<CodeStubDto> = emptySet(),
	/** Soft delete (unix epoch in ms) timestamp of the object. */
	override val endOfLife: Long? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	override val deletionDate: Long? = null,
	/** The type of the maintenance task. */
	val taskType: String? = null,
	/** Extra properties for the maintenance task. */
	val properties: Set<PropertyStubDto> = emptySet(),
	/** The current status of the maintenance task (pending, ongoing, cancelled, completed). */
	@param:Schema(defaultValue = "TaskStatusDto.pending")
	val status: TaskStatusDto = TaskStatusDto.pending,
	/** The secret patient key, encrypted in the patient's own AES key. */
	override val secretForeignKeys: Set<String> = emptySet(),
	/** The patient id encrypted in the delegates' AES keys. */
	override val cryptedForeignKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	/** The delegations giving access to connected healthcare information. */
	override val delegations: Map<String, Set<DelegationDto>> = emptyMap(),
	/** The keys used to encrypt this entity when stored encrypted. */
	override val encryptionKeys: Map<String, Set<DelegationDto>> = emptyMap(),
	/** The base64-encoded encrypted fields of this entity. */
	override val encryptedSelf: Base64StringDto? = null,
	/** The security metadata of the entity. */
	override val securityMetadata: SecurityMetadataDto? = null,
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
