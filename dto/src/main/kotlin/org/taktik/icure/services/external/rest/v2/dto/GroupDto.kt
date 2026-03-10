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

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = """This entity represents a group""")
/**
 * Represents a group in the iCure platform. A group corresponds to a practice, hospital, or organization
 * that contains its own set of databases and users.
 */
data class GroupDto(
	/** The id of the group. We encourage using either a v4 UUID or a HL7 Id. */
	@param:Schema(description = "The id of the group. We encourage using either a v4 UUID or a HL7 Id.") override val id: String,
	/** The revision of the group in the database, used for conflict management / optimistic locking. */
	@param:Schema(description = "The revision of the group in the database, used for conflict management / optimistic locking.") override val rev: String? = null,
	/** Hard delete (unix epoch in ms) timestamp of the object. */
	override val deletionDate: Long? = null,
	/** Tags that qualify the group as being member of a certain class. */
	override val tags: Set<CodeStubDto> = emptySet(),
	/** Tags that are publicly visible for the group. */
	@param:JsonInclude(JsonInclude.Include.NON_EMPTY) val publicTags: Set<CodeStubDto> = emptySet(),
	/** Username for the group. */
	@param:Schema(description = "Username for the group") val name: String? = null,
	/** Password for the group access. */
	@param:Schema(description = "Password for the group access") val password: String? = null,
	/** List of servers accessible to the group. */
	@param:Schema(description = "List of servers accessible to the group") val servers: List<String>? = null,
	/** Whether the group has a super admin permission. */
	@param:Schema(description = "Whether the group has a super admin permission, originally set to no access.") val superAdmin: Boolean = false,
	/** Extra properties for the group. Those properties are typed (see class Property). */
	@param:Schema(description = "Extra properties for the user. Those properties are typed (see class Property)") val properties: Set<PropertyStubDto> = emptySet(),
	/** The default roles for each user type, if not otherwise specified on the user. */
	@param:Schema(description = "The default roles for each user type, if not otherwise specified on the user.") val defaultUserRoles: Map<UserTypeDto, Set<String>> = emptyMap(),
	/** Single-used token to perform specific operations. */
	@param:Schema(description = "Single-used token to perform specific operations") val operationTokens: Map<String, OperationTokenDto> = emptyMap(),
	@param:Schema(
		description = "List of entities that have to be collected from a shared database. Only Code and tarification can be set at this point.",
	/** List of entities that have to be collected from a shared database. */
	) val sharedEntities: Map<String, String> = emptyMap(),
	/** Minimum version of Kraken required to access API. */
	@param:Schema(description = "Minimum version of Kraken required to access API") val minimumKrakenVersion: String? = null,
	@param:JsonInclude(
		JsonInclude.Include.NON_EMPTY,
	)
	/** Verified public keys that can be used to allow log in with external JWTs. */
	@param:Schema(description = "Verified public keys that can be used to allow log in with external JWTs") val externalJwtConfig: Map<String, ExternalJwtConfigDto> = emptyMap(),
	/** The minimum authentication class required for elevated privileges. */
	val minimumAuthenticationClassForElevatedPrivileges: AuthenticationClassDto = AuthenticationClassDto.PASSWORD,
	/** The id of the parent super group, if any. */
	val superGroup: String? = null,
	@param:Schema(
		description = "A user-chosen identifier for the applications for which this group holds data. Helps to isolate environments when working with multi-group applications.",
	)
	/** A user-chosen identifier for the applications for which this group holds data. */
	@param:JsonAlias("projectId")
	@SdkName("projectId")
	val applicationId: String? = null,
	val templates: TemplatesConfigurationDto? = null,
) : StoredDocumentDto,
	HasTagsDto {
	override fun withIdRev(
		id: String?,
		rev: String,
	) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)

	data class TemplatesConfigurationDto(
		val specId: String,
		val emailSender: String? = null,
		val smsSender: String? = null,
		val emailVerificationTemplateId: String? = null,
		val mobilePhoneVerificationTemplateId: String? = null,
		val existingEmailNotificationTemplateId: String? = null,
		val existingMobilePhoneNotificationTemplateId: String? = null,
	)
}
