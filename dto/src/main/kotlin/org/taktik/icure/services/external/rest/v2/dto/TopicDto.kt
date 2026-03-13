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
import org.taktik.icure.services.external.rest.v2.dto.base.ICureDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.DelegationDto
import org.taktik.icure.services.external.rest.v2.dto.embed.EncryptableDto
import org.taktik.icure.services.external.rest.v2.dto.embed.SecurityMetadataDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto

/**
 * Defines the possible roles a participant can have within a topic conversation.
 */
enum class TopicRoleDto {
	PARTICIPANT,
	ADMIN,
	OWNER,
}

@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents a topic for secure messaging between healthcare parties. A topic groups messages in a conversation
 * and can be linked to health elements and services for medical context.
 */
data class TopicDto(
	/** The unique identifier of the topic. */
	override val id: String,
	/** The revision of the topic in the database, used for conflict management / optimistic locking. */
	override val rev: String? = null,
	/** The timestamp (unix epoch in ms) of creation. */
	override val created: Long? = null,
	/** The timestamp (unix epoch in ms) of the latest modification. */
	override val modified: Long? = null,
	/** The id of the health element linked to this topic. */
	val healthElementId: String? = null,
	/** The id of the contact linked to this topic. */
	val contactId: String? = null,
	/** A description of the topic. */
	val description: String? = null,
	/** Codes that identify or qualify this particular topic. */
	override val codes: Set<CodeStubDto> = emptySet(),
	/** Tags that qualify the topic as being member of a certain class. */
	override val tags: Set<CodeStubDto> = emptySet(),
	/** The id of the User that created this topic. */
	override val author: String? = null,
	/** The id of the HealthcareParty that is responsible for this topic. */
	override val responsible: String? = null,
	/** The id of the medical location where this topic was created. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	override val medicalLocationId: String? = null,
	/** Soft delete (unix epoch in ms) timestamp of the object. */
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	val endOfLife: Long? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	override val deletionDate: Long? = null,
	/** Map of active participants with their roles (participant, admin, or owner). */
	val activeParticipants: Map<String, TopicRoleDto> = emptyMap(),
	/** The security metadata of the entity. */
	override val securityMetadata: SecurityMetadataDto? = null,
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
	/** Set of ids of health elements linked to this topic. */
	val linkedHealthElements: Set<String> = emptySet(),
	/** Set of ids of services linked to this topic. */
	val linkedServices: Set<String> = emptySet(),
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
