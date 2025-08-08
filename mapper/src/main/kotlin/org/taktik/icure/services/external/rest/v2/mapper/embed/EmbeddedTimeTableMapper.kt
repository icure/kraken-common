package org.taktik.icure.services.external.rest.v2.mapper.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.embed.EmbeddedTimeTableHour
import org.taktik.icure.entities.embed.EmbeddedTimeTableItem
import org.taktik.icure.entities.embed.ResourceGroupAllocationSchedule
import org.taktik.icure.services.external.rest.v2.dto.embed.EmbeddedTimeTableHourDto
import org.taktik.icure.services.external.rest.v2.dto.embed.EmbeddedTimeTableItemDto
import org.taktik.icure.services.external.rest.v2.dto.embed.ResourceGroupAllocationScheduleDto
import org.taktik.icure.services.external.rest.v2.mapper.base.CodeStubV2Mapper

@Mapper(componentModel = "spring", uses = [EmbeddedTimeTableItemV2Mapper::class, CodeStubV2Mapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface ResourceGroupAllocationV2Mapper {
	fun map(resourceGroupAllocationScheduleDto: ResourceGroupAllocationScheduleDto): ResourceGroupAllocationSchedule
	fun map(embeddedTimeTable: ResourceGroupAllocationSchedule): ResourceGroupAllocationScheduleDto
}

@Mapper(componentModel = "spring", uses = [EmbeddedTimeTableHourV2Mapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface EmbeddedTimeTableItemV2Mapper {
	fun map(embeddedTimeTableDto: EmbeddedTimeTableItemDto): EmbeddedTimeTableItem
	fun map(embeddedTimeTable: EmbeddedTimeTableItem): EmbeddedTimeTableItemDto
}

@Mapper(componentModel = "spring", uses = [], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface EmbeddedTimeTableHourV2Mapper {
	fun map(embeddedTimeTableDto: EmbeddedTimeTableHourDto): EmbeddedTimeTableHour
	fun map(embeddedTimeTable: EmbeddedTimeTableHour): EmbeddedTimeTableHourDto
}
