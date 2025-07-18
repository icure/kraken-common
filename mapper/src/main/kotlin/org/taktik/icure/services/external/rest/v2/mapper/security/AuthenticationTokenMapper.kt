package org.taktik.icure.services.external.rest.v2.mapper.security

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.security.AuthenticationToken
import org.taktik.icure.services.external.rest.InstantMapper
import org.taktik.icure.services.external.rest.v2.dto.security.AuthenticationTokenDto

@Mapper(componentModel = "spring", uses = [InstantMapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface UnsecureAuthenticationTokenV2Mapper {
	@Mappings(
		Mapping(target = "token", expression = """kotlin(requireNotNull(authenticationTokenDto.token) { "Token is missing" })"""),
	)
	fun map(authenticationTokenDto: AuthenticationTokenDto): AuthenticationToken

	// Deletion date is not mapped back because the corresponding tokens will be deleted
	fun map(authenticationToken: AuthenticationToken): AuthenticationTokenDto
}
