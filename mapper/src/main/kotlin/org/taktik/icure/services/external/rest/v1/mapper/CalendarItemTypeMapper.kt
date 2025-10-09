/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.CalendarItemType
import org.taktik.icure.services.external.rest.v1.dto.CalendarItemTypeDto
import org.taktik.icure.services.external.rest.v1.mapper.embed.DurationConfigMapper

@Mapper(componentModel = "spring", uses = [DurationConfigMapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface CalendarItemTypeMapper {
	@Mappings(
		Mapping(target = "attachments", ignore = true),
		Mapping(target = "revHistory", ignore = true),
		Mapping(target = "conflicts", ignore = true),
		Mapping(target = "revisionsInfo", ignore = true),
	)
	fun map(calendarItemTypeDto: CalendarItemTypeDto): CalendarItemType
	fun map(calendarItemType: CalendarItemType): CalendarItemTypeDto
}
