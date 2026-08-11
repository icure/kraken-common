package org.taktik.icure.services.external.rest.v2.mapper.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.embed.HealthElementAsserter
import org.taktik.icure.services.external.rest.v2.dto.embed.HealthElementAsserterDto

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface HealthElementAsserterV2Mapper {
	fun map(healthElementAsserterDto: HealthElementAsserterDto): HealthElementAsserter
	fun map(healthElementAsserter: HealthElementAsserter): HealthElementAsserterDto
}
