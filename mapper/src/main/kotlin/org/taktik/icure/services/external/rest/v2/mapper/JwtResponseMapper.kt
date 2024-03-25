package org.taktik.icure.services.external.rest.v2.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.security.jwt.JwtResponse
import org.taktik.icure.services.external.rest.v2.dto.security.jwt.JwtResponseDto

@Mapper(componentModel = "spring", uses = [], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface JwtResponseV2Mapper {
	fun map(jwtResponseDto: JwtResponseDto): JwtResponse
	fun map(jwtResponse: JwtResponse): JwtResponseDto
}
