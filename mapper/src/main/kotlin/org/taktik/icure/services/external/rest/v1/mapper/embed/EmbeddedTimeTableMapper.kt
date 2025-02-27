package org.taktik.icure.services.external.rest.v1.mapper.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.embed.EmbeddedTimeTable
import org.taktik.icure.services.external.rest.v1.dto.embed.EmbeddedTimeTableDto

@Mapper(componentModel = "spring", uses = [TimeTableItemMapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface EmbeddedTimeTableMapper {
	fun map(embeddedTimeTableDto: EmbeddedTimeTableDto): EmbeddedTimeTable
	fun map(embeddedTimeTable: EmbeddedTimeTable): EmbeddedTimeTableDto
}