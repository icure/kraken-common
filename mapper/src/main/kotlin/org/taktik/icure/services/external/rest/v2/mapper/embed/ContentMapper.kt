package org.taktik.icure.services.external.rest.v2.mapper.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.embed.Content
import org.taktik.icure.services.external.rest.v2.dto.embed.ContentDto

@Mapper(componentModel = "spring", uses = [ServiceV2Mapper::class, MeasureV2Mapper::class, MedicationV2Mapper::class, TimeSeriesV2Mapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface ContentV2Mapper {
	fun map(contentDto: ContentDto): Content
	fun map(content: Content): ContentDto
}
