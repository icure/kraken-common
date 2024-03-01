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
import org.taktik.icure.services.external.rest.v2.dto.base.EncryptableDto
import org.taktik.icure.services.external.rest.v2.dto.base.ICureDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.DelegationDto
import org.taktik.icure.services.external.rest.v2.dto.embed.SecurityMetadataDto
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto

enum class TopicRoleDto {
	PARTICIPANT,
	ADMIN,
	OWNER,
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class TopicDto(
    override val id: String,
    override val rev: String? = null,
    override val created: Long? = null,
    override val modified: Long? = null,
    val healthElementId: String? = null,
    val contactId: String? = null,
    val description: String? = null,
    override val codes: Set<CodeStubDto> = emptySet(),
    override val tags: Set<CodeStubDto> = emptySet(),
    override val author: String? = null,
    override val responsible: String? = null,
    override val medicalLocationId: String? = null,
    override val endOfLife: Long? = null,
    override val deletionDate: Long? = null,
    val activeParticipants: Map<String, TopicRoleDto> = emptyMap(),
    override val securityMetadata: SecurityMetadataDto? = null,
    override val secretForeignKeys: Set<String> = emptySet(),
    override val cryptedForeignKeys: Map<String, Set<DelegationDto>> = emptyMap(),
    override val delegations: Map<String, Set<DelegationDto>> = emptyMap(),
    override val encryptionKeys: Map<String, Set<DelegationDto>> = emptyMap(),
    override val encryptedSelf: Base64StringDto? = null,
    val linkedHealthElements: Set<String> = emptySet(),
    val linkedServices: Set<String> = emptySet(),
) : StoredDocumentDto, ICureDocumentDto<String>, EncryptableDto {
	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
