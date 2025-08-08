package org.taktik.icure.services.external.rest.v2.mapper.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.embed.SecureDelegation
import org.taktik.icure.services.external.rest.v2.dto.embed.SecureDelegationDto

@Mapper(componentModel = "spring", uses = [], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface SecureDelegationV2Mapper {
	fun map(secureDelegationDto: SecureDelegationDto): SecureDelegation

	fun map(secureDelegation: SecureDelegation): SecureDelegationDto

	fun mapSecureDelegationsMaps(delegations: Map<String, SecureDelegation>): Map<String, SecureDelegationDto> = delegations.mapValues { map(it.value) }

	fun mapSecureDelegationDtosMaps(delegations: Map<String, SecureDelegationDto>): Map<String, SecureDelegation> = delegations.mapValues { map(it.value) }
}
