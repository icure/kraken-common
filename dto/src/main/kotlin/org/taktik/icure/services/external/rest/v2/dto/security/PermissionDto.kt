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
package org.taktik.icure.services.external.rest.v2.dto.security

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents the combined set of granted and revoked permissions for a user or role.
 * Revocations take precedence over grants when both apply to the same permission type.
 */
data class PermissionDto(
	/** The set of permission items that are explicitly granted. */
	@param:Schema(description = "Granted permissions.") val grants: Set<PermissionItemDto> = emptySet(),
	/** The set of permission items that are explicitly revoked. */
	@param:Schema(description = "Revoked permissions.") val revokes: Set<PermissionItemDto> = emptySet(),
) : Cloneable,
	Serializable
