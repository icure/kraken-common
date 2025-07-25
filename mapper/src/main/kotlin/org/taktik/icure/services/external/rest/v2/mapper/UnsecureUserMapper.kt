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

package org.taktik.icure.services.external.rest.v2.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.User
import org.taktik.icure.entities.embed.Identifier
import org.taktik.icure.services.external.rest.v2.dto.UserDto
import org.taktik.icure.services.external.rest.v2.dto.security.LoginIdentifierDto
import org.taktik.icure.services.external.rest.v2.mapper.base.IdentifierV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.base.PropertyStubV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.security.PermissionV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.security.UnsecureAuthenticationTokenV2Mapper

@Mapper(componentModel = "spring", uses = [PermissionV2Mapper::class, PropertyStubV2Mapper::class, IdentifierV2Mapper::class, UnsecureAuthenticationTokenV2Mapper::class, UnsecureUserV2Mapper.SystemMetadataV2Mapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface UnsecureUserV2Mapper {
	@Mappings(
		Mapping(target = "attachments", ignore = true),
		Mapping(target = "revHistory", ignore = true),
		Mapping(target = "conflicts", ignore = true),
		Mapping(target = "revisionsInfo", ignore = true),
		Mapping(target = "applicationTokens", ignore = true),
	)
	fun map(userDto: UserDto): User

	@Mappings(
		Mapping(target = "applicationTokens", expression = "kotlin(emptyMap())"),
	)
	fun map(user: User): UserDto

	@Mapper(componentModel = "spring", uses = [], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
	interface SystemMetadataV2Mapper {
		fun map(metaDto: UserDto.SystemMetadata): User.SystemMetadata

		fun map(meta: User.SystemMetadata): UserDto.SystemMetadata

		fun map(loginIdentifierDto: LoginIdentifierDto): Identifier = Identifier(assigner = loginIdentifierDto.assigner, value = loginIdentifierDto.value)
		fun map(identifier: Identifier): LoginIdentifierDto = LoginIdentifierDto(
			assigner = checkNotNull(identifier.assigner) { "LoginIdentifier assigner cannot be null" },
			value = checkNotNull(identifier.value) { "LoginIdentifier value cannot be null" },
		)
	}
}
