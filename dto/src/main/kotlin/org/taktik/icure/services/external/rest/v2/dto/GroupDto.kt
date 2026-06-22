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

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.SdkName
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.base.HasTagsDto
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.AuthenticationClassDto
import org.taktik.icure.services.external.rest.v2.dto.embed.UserTypeDto
import org.taktik.icure.services.external.rest.v2.dto.security.ExternalJwtConfigDto
import org.taktik.icure.services.external.rest.v2.dto.security.OperationTokenDto
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = """This entity represents a group""")
/**
 * Represents a group in the iCure platform. A group corresponds to a practice, hospital, or organization
 * that contains its own set of databases and users.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.GroupDto")
data class GroupDto(
	/** The id of the group. We encourage using either a v4 UUID or a HL7 Id. */
	@param:Schema(description = "The id of the group. We encourage using either a v4 UUID or a HL7 Id.") override val id: String,
	/** The revision of the group in the database, used for conflict management / optimistic locking. */
	@param:Schema(description = "The revision of the group in the database, used for conflict management / optimistic locking.") override val rev: String? = null,
	/** Soft delete (unix epoch in ms) timestamp of the object. */
	override val deletionDate: Long? = null,
	/** Tags that qualify the group as being member of a certain class. */
	override val tags: Set<CodeStubDto> = emptySet(),
	/** Tags that are publicly visible for the group. */
	@param:JsonInclude(JsonInclude.Include.NON_EMPTY) @ActiveField val publicTags: Set<CodeStubDto> = emptySet(),
	/** Username for the group. */
	@param:Schema(description = "Username for the group") @ActiveField val name: String? = null,
	/** Password for the group access. */
	@param:Schema(description = "Password for the group access") @ActiveField val password: String? = null,
	/** List of servers accessible to the group. */
	@param:Schema(description = "List of servers accessible to the group") @ActiveField val servers: List<String>? = null,
	/** Whether the group has a super admin permission. */
	@param:Schema(description = "Whether the group has a super admin permission, originally set to no access.") @ActiveField val superAdmin: Boolean = false,
	/** Extra properties for the group. Those properties are typed (see class Property). */
	@param:Schema(description = "Extra properties for the user. Those properties are typed (see class Property)") @ActiveField val properties: Set<PropertyStubDto> = emptySet(),
	/** The default roles for each user type, if not otherwise specified on the user. */
	@param:Schema(description = "The default roles for each user type, if not otherwise specified on the user.") @ActiveField val defaultUserRoles: Map<UserTypeDto, Set<String>> = emptyMap(),
	/** Single-used token to perform specific operations. */
	@param:Schema(description = "Single-used token to perform specific operations") @ActiveField val operationTokens: Map<String, OperationTokenDto> = emptyMap(),
	@param:Schema(
		description = "List of entities that have to be collected from a shared database. Only Code and tarification can be set at this point.",
	/** List of entities that have to be collected from a shared database. */
	) @ActiveField val sharedEntities: Map<String, String> = emptyMap(),
	/** Minimum version of Kraken required to access API. */
	@param:Schema(description = "Minimum version of Kraken required to access API") @ActiveField val minimumKrakenVersion: String? = null,
	@param:JsonInclude(
		JsonInclude.Include.NON_EMPTY,
	)
	/** Verified public keys that can be used to allow log in with external JWTs. */
	@param:Schema(description = "Verified public keys that can be used to allow log in with external JWTs") @ActiveField val externalJwtConfig: Map<String, ExternalJwtConfigDto> = emptyMap(),
	/** The minimum authentication class required for elevated privileges. */
	@ActiveField val minimumAuthenticationClassForElevatedPrivileges: AuthenticationClassDto = AuthenticationClassDto.PASSWORD,
	/** The id of the parent super group, if any. */
	@ActiveField val superGroup: String? = null,
	@param:Schema(
		description = "A user-chosen identifier for the applications for which this group holds data. Helps to isolate environments when working with multi-group applications.",
	)
	/** A user-chosen identifier for the applications for which this group holds data. */
	@param:JsonAlias("projectId")
	@SdkName("projectId")
	@ActiveField val applicationId: String? = null,
	@ActiveField val templates: TemplatesConfigurationDto? = null,
	/**
	 * The versions of the custom design doc schema applied to the group.
	 */
	@ActiveField val designDocSchemaVersions: Set<Int> = emptySet(),
	/**
	 * The version of the custom design doc schema to apply by default children groups on creation.
	 */
	@ActiveField val defaultChildrenSchemaVersion: Int? = null,
) : StoredDocumentDto,
	HasTagsDto {
	override fun withIdRev(
		id: String?,
		rev: String,
	) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)

	@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.GroupDto.TemplatesConfigurationDto")
	data class TemplatesConfigurationDto(
		@ActiveField val specId: String,
		@ActiveField val emailSender: String? = null,
		@ActiveField val smsSender: String? = null,
		@ActiveField val emailVerificationTemplateId: String? = null,
		@ActiveField val mobilePhoneVerificationTemplateId: String? = null,
		@ActiveField val existingEmailNotificationTemplateId: String? = null,
		@ActiveField val existingMobilePhoneNotificationTemplateId: String? = null,
	)
}
