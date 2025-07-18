/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.embed.Delegation
import org.taktik.icure.services.external.rest.v1.dto.embed.DelegationDto

@Mapper(componentModel = "spring", uses = [], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface DelegationMapper {
	fun map(delegationDto: DelegationDto): Delegation
	fun map(delegation: Delegation): DelegationDto

	fun mapDelegationsMaps(delegations: Map<String, Set<Delegation>>): Map<String, Set<DelegationDto>> = delegations.mapValues { it.value.map { map(it) }.toSet() }

	fun mapDelegationDtosMaps(delegations: Map<String, Set<DelegationDto>>): Map<String, Set<Delegation>> = delegations.mapValues { it.value.map { map(it) }.toSet() }

	fun mapDelegations(delegations: Set<Delegation>): Set<DelegationDto> = delegations.map { map(it) }.toSet()

	fun mapDelegationDtos(delegations: Set<DelegationDto>): Set<Delegation> = delegations.map { map(it) }.toSet()
}
