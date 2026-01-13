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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v2.dto.base.IdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.base.PrincipalDto
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.DelegationTagDto
import org.taktik.icure.services.external.rest.v2.dto.enums.UsersStatusDto
import org.taktik.icure.services.external.rest.v2.dto.enums.UsersTypeDto
import org.taktik.icure.services.external.rest.v2.dto.security.AuthenticationTokenDto
import org.taktik.icure.services.external.rest.v2.dto.security.LoginIdentifierDto
import org.taktik.icure.services.external.rest.v2.dto.security.PermissionDto
import org.taktik.icure.utils.InstantDeserializer
import org.taktik.icure.utils.InstantSerializer
import java.io.Serializable
import java.time.Instant

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
	description = """This entity is a root level object. It represents an user that can log in to the iCure platform. It is serialized in JSON and saved in the underlying icure-base CouchDB database.""",
)
data class UserDto(
	@param:Schema(description = "the Id of the user. We encourage using either a v4 UUID or a HL7 Id.") override val id: String,
	@param:Schema(description = "the revision of the user in the database, used for conflict management / optimistic locking.") override val rev: String? = null,
	override val deletionDate: Long? = null,
	val created: Long? = null,
	val identifier: List<IdentifierDto> = listOf(),
	@param:Schema(description = "Last name of the user. This is the official last name that should be used for official administrative purposes.")
	override val name: String? = null,
	@param:Schema(description = "Extra properties for the user. Those properties are typed (see class Property)") override val properties: Set<PropertyStubDto> = emptySet(),
	@param:Schema(description = "Local permissions specified for the user: these may not reflect the actual permissions the user has on the cloud system")
	val permissions: Set<PermissionDto> = emptySet(),
	@param:Schema(description = "Local roles specified for the user: these may not reflect the actual permissions the user has on the cloud system")
	val roles: Set<String> = emptySet(),
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@param:Schema(description = "Authorization source for user. 'Database', 'ldap' or 'token'") val type: UsersTypeDto? = null,
	@param:Schema(description = "State of user's activeness: 'Active', 'Disabled' or 'Registering'") val status: UsersStatusDto? = null,
	@param:Schema(description = "Username for this user. We encourage using an email address") val login: String? = null,
	@param:Schema(description = "Hashed version of the password (BCrypt is used for hashing)") val passwordHash: String? = null,
	@param:Schema(description = "id of the group (practice/hospital) the user is member of") val groupId: String? = null,
	@param:Schema(description = "Id of the healthcare party if the user is a healthcare party.") val healthcarePartyId: String? = null,
	@param:Schema(description = "Id of the patient if the user is a patient") val patientId: String? = null,
	@param:Schema(description = "Id of the device if the user is a device") val deviceId: String? = null,
	@param:Schema(description = "Delegations that are automatically generated client side when a new database object is created by this user")
	val autoDelegations: Map<DelegationTagDto, Set<String>> = emptyMap(), // DelegationTagDto -> healthcarePartyIds
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@param:JsonSerialize(using = InstantSerializer::class)
	@param:JsonInclude(JsonInclude.Include.NON_NULL)
	@param:JsonDeserialize(using = InstantDeserializer::class)
	@param:Schema(description = "the timestamp (unix epoch in ms) of creation of the user, will be filled automatically if missing. Not enforced by the application server.")
	val createdDate: Instant? = null,
	@param:JsonSerialize(using = InstantSerializer::class)
	@param:JsonInclude(JsonInclude.Include.NON_NULL)
	@param:JsonDeserialize(using = InstantDeserializer::class)
	@param:Schema(
		description = "the timestamp (unix epoch in ms) of the latest validation of the terms of use of the application",
	) val termsOfUseDate: Instant? = null,
	@param:Schema(description = "email address of the user (used for token exchange or password recovery).") val email: String? = null,
	@param:Schema(description = "mobile phone of the user (used for token exchange or password recovery).") val mobilePhone: String? = null,
	@get:Deprecated("Long lived authentication tokens used for inter-applications authentication")
	val applicationTokens: Map<String, String> = emptyMap(),
	@param:Schema(description = "Encrypted and time-limited Authentication tokens used for inter-applications authentication")
	val authenticationTokens: Map<String, AuthenticationTokenDto> = emptyMap(),
	@param:Schema(description = "Metadata used to enrich the user with information from the cloud environment. This value can't be modified as part of the user changes, you have to instead use the appropriate endpoints.")
	val systemMetadata: SystemMetadata? = null,
) : StoredDocumentDto,
	PrincipalDto,
	Cloneable,
	Serializable {
	override fun withIdRev(
		id: String?,
		rev: String,
	) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)

	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown = true)
	data class SystemMetadata(
		@param:Schema(description = "The roles that the user for acting on the cloud environment.", required = true)
		val roles: Set<String>,
		@param:Schema(
			description = "Specifies if the user is an admin in the cloud environment. An admin user is considered to have the permissions to do anything on his group and on children groups.",
			required = true,
		)
		val isAdmin: Boolean,
		@param:Schema(
			description = "Specifies if the roles of the user are inherited from the group configuration (true), or if they are custom for the user (false).",
			required = true,
		)
		val inheritsRoles: Boolean,
		@param:Schema(description = "Identifiers of the user available for login")
		@param:JsonInclude(JsonInclude.Include.NON_EMPTY) val loginIdentifiers: List<LoginIdentifierDto> = emptyList(),
	) : Serializable
}
