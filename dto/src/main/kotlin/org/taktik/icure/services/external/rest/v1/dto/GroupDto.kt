/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v1.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v1.dto.base.HasTagsDto
import org.taktik.icure.services.external.rest.v1.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v1.dto.embed.AuthenticationClassDto
import org.taktik.icure.services.external.rest.v1.dto.embed.UserTypeDto
import org.taktik.icure.services.external.rest.v1.dto.security.ExternalJwtConfigDto
import org.taktik.icure.services.external.rest.v1.dto.security.OperationTokenDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = """This entity represents a group""")
data class GroupDto(
	@param:Schema(description = "The id of the group. We encourage using either a v4 UUID or a HL7 Id.") override val id: String,
	@param:Schema(description = "The revision of the group in the database, used for conflict management / optimistic locking.") override val rev: String? = null,
	override val deletionDate: Long? = null,
	override val tags: Set<CodeStubDto> = emptySet(),
	val publicTags: Set<CodeStubDto> = emptySet(),
	@param:Schema(description = "Username for the group") val name: String? = null,
	@param:Schema(description = "Password for the group access") val password: String? = null,
	@param:Schema(description = "List of servers accessible to the group") val servers: List<String>? = null,
	@param:Schema(description = "Whether the group has a super admin permission, originally set to no access.") val superAdmin: Boolean = false,
	@param:Schema(description = "Extra properties for the user. Those properties are typed (see class Property)") val properties: Set<PropertyStubDto> = emptySet(),
	@param:Schema(description = "The default roles for each user type, if not otherwise specified on the user.") val defaultUserRoles: Map<UserTypeDto, Set<String>> = emptyMap(),
	@param:Schema(description = "Single-used token to perform specific operations") val operationTokens: Map<String, OperationTokenDto> = emptyMap(),
	@param:Schema(
		description = "List of entities that have to be collected from a shared database. Only Code and tarification can be set at this point.",
	) val sharedEntities: Map<String, String> = emptyMap(),
	@param:Schema(description = "Minimum version of Kraken required to access API") val minimumKrakenVersion: String? = null,
	@param:Schema(description = "Verified public keys that can be used to allow log in with external JWTs") val externalJwtConfig: Map<String, ExternalJwtConfigDto> = emptyMap(),
	val customEntityConfig: CustomEntityConfiguration? = null,
	val minimumAuthenticationClassForElevatedPrivileges: AuthenticationClassDto = AuthenticationClassDto.PASSWORD,
	val superGroup: String? = null,
	@param:Schema(
		description = "A user-chosen identifier for the applications for which this group holds data. Helps to isolate environments when working with multi-group applications.",
	)
	val applicationId: String? = null,
) : StoredDocumentDto,
	HasTagsDto {
	data class CustomEntityConfiguration(
		val sourceGroup: String,
		val version: Int
	)

	override fun withIdRev(
		id: String?,
		rev: String,
	) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
