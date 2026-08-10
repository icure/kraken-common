package org.taktik.icure.services.external.rest.v2.mapper.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.embed.HealthElementAsserter
import org.taktik.icure.services.external.rest.v2.dto.embed.HealthElementAsserterDto

@Mapper(componentModel = "spring", uses = [], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface HealthElementAsserterV2Mapper {
	fun map(dto: HealthElementAsserterDto): HealthElementAsserter = when (dto) {
		is HealthElementAsserterDto.Patient -> map(dto)
		is HealthElementAsserterDto.RelatedPerson -> map(dto)
		is HealthElementAsserterDto.HealthcareParty -> map(dto)
	}
	fun map(obj: HealthElementAsserter): HealthElementAsserterDto = when (obj) {
		is HealthElementAsserter.Patient -> map(obj)
		is HealthElementAsserter.RelatedPerson -> map(obj)
		is HealthElementAsserter.HealthcareParty -> map(obj)
	}

	fun map(dto: HealthElementAsserterDto.Patient): HealthElementAsserter.Patient
	fun map(obj: HealthElementAsserter.Patient): HealthElementAsserterDto.Patient

	fun map(dto: HealthElementAsserterDto.RelatedPerson): HealthElementAsserter.RelatedPerson
	fun map(obj: HealthElementAsserter.RelatedPerson): HealthElementAsserterDto.RelatedPerson

	fun map(dto: HealthElementAsserterDto.HealthcareParty): HealthElementAsserter.HealthcareParty
	fun map(obj: HealthElementAsserter.HealthcareParty): HealthElementAsserterDto.HealthcareParty
}
