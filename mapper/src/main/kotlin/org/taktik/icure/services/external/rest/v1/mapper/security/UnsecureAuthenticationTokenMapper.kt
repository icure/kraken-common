package org.taktik.icure.services.external.rest.v1.mapper.security

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.security.AuthenticationToken
import org.taktik.icure.services.external.rest.InstantMapper
import org.taktik.icure.services.external.rest.v1.dto.security.AuthenticationTokenDto

@Mapper(componentModel = "spring", uses = [InstantMapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface UnsecureAuthenticationTokenMapper {
	@Mappings(
		Mapping(target = "token", expression = """kotlin(requireNotNull(authenticationTokenDto.token) { "Token is missing" })"""),
	)
	fun map(authenticationTokenDto: AuthenticationTokenDto): AuthenticationToken
	fun map(authenticationToken: AuthenticationToken): AuthenticationTokenDto
}
