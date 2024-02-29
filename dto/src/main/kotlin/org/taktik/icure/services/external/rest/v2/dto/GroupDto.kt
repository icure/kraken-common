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
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.base.HasTagsDto
import org.taktik.icure.services.external.rest.v2.dto.base.StoredDocumentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.AuthenticationClassDto
import org.taktik.icure.services.external.rest.v2.dto.embed.UserTypeDto
import org.taktik.icure.services.external.rest.v2.dto.security.OperationTokenDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = """This entity represents a group""")
data class GroupDto(
	@Schema(description = "The id of the group. We encourage using either a v4 UUID or a HL7 Id.") override val id: String,
	@Schema(description = "The revision of the group in the database, used for conflict management / optimistic locking.") override val rev: String? = null,
	override val deletionDate: Long? = null,
	override val tags: Set<CodeStubDto> = emptySet(),

	@Schema(description = "Username for the group") val name: String? = null,
	@Schema(description = "Password for the group access") val password: String? = null,
	@Schema(description = "List of servers accessible to the group") val servers: List<String>? = null,
	@Schema(description = "Whether the group has a super admin permission, originally set to no access.") val superAdmin: Boolean = false,
	@Schema(description = "Extra properties for the user. Those properties are typed (see class Property)") val properties: Set<PropertyStubDto> = emptySet(),
	@Schema(description = "The default roles for each user type, if not otherwise specified on the user.") val defaultUserRoles: Map<UserTypeDto, Set<String>> = emptyMap(),
	@Schema(description = "Single-used token to perform specific operations") val operationTokens: Map<String, OperationTokenDto> = emptyMap(),
	@Schema(description = "List of entities that have to be collected from a shared database. Only Code and tarification can be set at this point.") val sharedEntities: Map<String, String> = emptyMap(),
	@Schema(description = "Minimum version of Kraken required to access API")	val minimumKrakenVersion: String? = null,
	val minimumAuthenticationClassForElevatedPrivileges: AuthenticationClassDto = AuthenticationClassDto.PASSWORD,

	val superGroup: String? = null
) : StoredDocumentDto, HasTagsDto {
	override fun withIdRev(id: String?, rev: String) = if (id != null) this.copy(id = id, rev = rev) else this.copy(rev = rev)
	override fun withDeletionDate(deletionDate: Long?) = this.copy(deletionDate = deletionDate)
}
