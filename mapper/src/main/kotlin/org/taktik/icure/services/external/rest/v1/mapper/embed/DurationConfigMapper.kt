package org.taktik.icure.services.external.rest.v1.mapper.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.CalendarItemType
import org.taktik.icure.services.external.rest.v1.dto.CalendarItemTypeDto

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface DurationConfigMapper {
	fun map(
		durationConfig: CalendarItemType.DurationConfig
	): CalendarItemTypeDto.DurationConfigDto = when (durationConfig) {
		is CalendarItemType.DurationConfig.DurationFormula -> map(durationConfig)
		is CalendarItemType.DurationConfig.DurationList -> map(durationConfig)
	}
	fun map(
		durationConfigDto: CalendarItemTypeDto.DurationConfigDto
	): CalendarItemType.DurationConfig = when (durationConfigDto) {
		is CalendarItemTypeDto.DurationConfigDto.DurationFormula -> map(durationConfigDto)
		is CalendarItemTypeDto.DurationConfigDto.DurationList -> map(durationConfigDto)
	}

	fun map(
		durationConfig: CalendarItemType.DurationConfig.DurationList
	): CalendarItemTypeDto.DurationConfigDto.DurationList
	fun map(
		durationConfigDto: CalendarItemTypeDto.DurationConfigDto.DurationList
	): CalendarItemType.DurationConfig.DurationList

	fun map(
		durationConfig: CalendarItemType.DurationConfig.DurationFormula
	): CalendarItemTypeDto.DurationConfigDto.DurationFormula
	fun map(
		durationConfigDto: CalendarItemTypeDto.DurationConfigDto.DurationFormula
	): CalendarItemType.DurationConfig.DurationFormula
}