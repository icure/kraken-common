/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto

/**
 * Created by aduchate on 18/07/13, 13:06
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v1.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v1.dto.base.EncryptableDto
import org.taktik.icure.services.external.rest.v1.dto.base.ICureDocumentDto
import org.taktik.icure.services.external.rest.v1.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v1.dto.embed.DelegationDto
import org.taktik.icure.services.external.rest.v1.dto.embed.SecurityMetadataDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class FormDto(
	@Schema(description = "the Id of the form. We encourage using either a v4 UUID or a HL7 Id.") override val id: String,
	@Schema(description = "the revision of the form in the database, used for conflict management / optimistic locking.") override val rev: String? = null,
	override val created: Long? = null,
	override val modified: Long? = null,
	override val author: String? = null,
	override val responsible: String? = null,
	override val medicalLocationId: String? = null,
	override val tags: Set<CodeStubDto> = emptySet(),
	override val codes: Set<CodeStubDto> = emptySet(),
	override val endOfLife: Long? = null,
	override val deletionDate: Long? = null,

	val openingDate: Long? = null, // YYYYMMDDHHMMSS if unknown, 00, ex:20010800000000. Note that to avoid all confusion: 2015/01/02 00:00:00 is encoded as 20150101235960.
	val status: String? = null,
	val version: Int? = null,
	val logicalUuid: String? = null,
	@Schema(description = "Name/basic description of the form") val descr: String? = null,
	@Schema(description = "A unique external id (from another external source).") val uniqueId: String? = null,
	@Schema(description = "Id of the form template being used to display the form") val formTemplateId: String? = null,
	@Schema(description = "Id of the contact for which the form is being used.") val contactId: String? = null,
	@Schema(description = "The healthcare element to which this form is attached.") val healthElementId: String? = null,
	@Schema(description = "The healthcare approach to which this form is attached.") val planOfActionId: String? = null,
	@Schema(description = "The parent of this form, used to determine the forms hierarchy") val parent: String? = null,

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
