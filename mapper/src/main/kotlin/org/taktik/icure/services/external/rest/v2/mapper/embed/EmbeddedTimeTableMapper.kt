package org.taktik.icure.services.external.rest.v2.mapper.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.embed.EmbeddedTimeTable
import org.taktik.icure.services.external.rest.v2.dto.embed.EmbeddedTimeTableDto

@Mapper(componentModel = "spring", uses = [TimeTableItemV2Mapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface EmbeddedTimeTableV2Mapper {
	fun map(embeddedTimeTableDto: EmbeddedTimeTableDto): EmbeddedTimeTable
	fun map(embeddedTimeTable: EmbeddedTimeTable): EmbeddedTimeTableDto
}