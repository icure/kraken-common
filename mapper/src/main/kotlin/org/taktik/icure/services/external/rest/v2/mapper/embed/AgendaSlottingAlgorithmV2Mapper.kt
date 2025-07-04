package org.taktik.icure.services.external.rest.v2.mapper.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.embed.AgendaSlottingAlgorithm
import org.taktik.icure.services.external.rest.v2.dto.embed.AgendaSlottingAlgorithmDto

@Mapper(componentModel = "spring", uses = [], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface AgendaSlottingAlgorithmV2Mapper {
	fun map(dto: AgendaSlottingAlgorithmDto): AgendaSlottingAlgorithm = when (dto) {
		is AgendaSlottingAlgorithmDto.FixedIntervals -> map(dto)
	}
	fun map(obj: AgendaSlottingAlgorithm): AgendaSlottingAlgorithmDto= when (obj) {
		is AgendaSlottingAlgorithm.FixedIntervals -> map(obj)
	}

	fun map(dto: AgendaSlottingAlgorithmDto.FixedIntervals): AgendaSlottingAlgorithm.FixedIntervals
	fun map(obj: AgendaSlottingAlgorithm.FixedIntervals): AgendaSlottingAlgorithmDto.FixedIntervals
}