package org.taktik.icure.services.external.rest.v2.mapper.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.CalendarItemType
import org.taktik.icure.services.external.rest.v2.dto.CalendarItemTypeDto

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface DurationConfigV2Mapper {
	fun map(
		durationConfig: CalendarItemType.DurationConfig
	): CalendarItemTypeDto.DurationConfigDto = when (durationConfig) {
		is CalendarItemType.DurationConfig.Formula -> map(durationConfig)
		is CalendarItemType.DurationConfig.Set -> map(durationConfig)
	}
	fun map(
		durationConfigDto: CalendarItemTypeDto.DurationConfigDto
	): CalendarItemType.DurationConfig = when (durationConfigDto) {
		is CalendarItemTypeDto.DurationConfigDto.Formula -> map(durationConfigDto)
		is CalendarItemTypeDto.DurationConfigDto.Set -> map(durationConfigDto)
	}

	fun map(
		durationConfig: CalendarItemType.DurationConfig.Set
	): CalendarItemTypeDto.DurationConfigDto.Set
	fun map(
		durationConfigDto: CalendarItemTypeDto.DurationConfigDto.Set
	): CalendarItemType.DurationConfig.Set

	fun map(
		durationConfig: CalendarItemType.DurationConfig.Formula
	): CalendarItemTypeDto.DurationConfigDto.Formula
	fun map(
		durationConfigDto: CalendarItemTypeDto.DurationConfigDto.Formula
	): CalendarItemType.DurationConfig.Formula
}