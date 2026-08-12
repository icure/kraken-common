package org.taktik.icure.services.external.rest.v2.mapper.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.embed.HealthElementAsserter
import org.taktik.icure.services.external.rest.v2.dto.embed.HealthElementAsserterDto
import org.taktik.icure.services.external.rest.v2.mapper.base.IdentifierV2Mapper

@Mapper(componentModel = "spring", uses = [IdentifierV2Mapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface HealthElementAsserterV2Mapper {
	fun map(healthElementAsserterDto: HealthElementAsserterDto): HealthElementAsserter
	fun map(healthElementAsserter: HealthElementAsserter): HealthElementAsserterDto

	fun map(localAsserterIdentifierDto: HealthElementAsserterDto.LocalAsserterIdentifier): HealthElementAsserter.LocalAsserterIdentifier
	fun map(localAsserterIdentifier: HealthElementAsserter.LocalAsserterIdentifier): HealthElementAsserterDto.LocalAsserterIdentifier
}
