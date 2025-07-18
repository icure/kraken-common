package org.taktik.icure.services.external.rest.v1.mapper.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.embed.Content
import org.taktik.icure.services.external.rest.v1.dto.embed.ContentDto

@Mapper(componentModel = "spring", uses = [ServiceMapper::class, MeasureMapper::class, MedicationMapper::class, TimeSeriesMapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface ContentMapper {
	fun map(contentDto: ContentDto): Content
	fun map(content: Content): ContentDto
}
