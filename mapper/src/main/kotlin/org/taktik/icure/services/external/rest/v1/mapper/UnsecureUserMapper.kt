/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.User
import org.taktik.icure.services.external.rest.v1.dto.UserDto
import org.taktik.icure.services.external.rest.v1.mapper.base.IdentifierMapper
import org.taktik.icure.services.external.rest.v1.mapper.base.PropertyStubMapper
import org.taktik.icure.services.external.rest.v1.mapper.security.UnsecureAuthenticationTokenMapper
import org.taktik.icure.services.external.rest.v1.mapper.security.PermissionMapper

@Mapper(componentModel = "spring", uses = [PermissionMapper::class, PropertyStubMapper::class, IdentifierMapper::class, UnsecureAuthenticationTokenMapper::class, UnsecureUserMapper.SystemMetadataMapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface UnsecureUserMapper {
	@Mappings(
		Mapping(target = "attachments", ignore = true),
		Mapping(target = "revHistory", ignore = true),
		Mapping(target = "conflicts", ignore = true),
		Mapping(target = "revisionsInfo", ignore = true),
	)
	fun map(userDto: UserDto): User

	@Mappings(
		Mapping(target = "applicationTokens", expression = "kotlin(emptyMap())"),
	)
	fun map(user: User): UserDto

	@Mapper(componentModel = "spring", uses = [], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
	interface SystemMetadataMapper {
		fun map(metaDto: UserDto.SystemMetadata): User.SystemMetadata

		fun map(meta: User.SystemMetadata): UserDto.SystemMetadata
	}
}
