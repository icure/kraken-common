/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.embed.TimeTableItem
import org.taktik.icure.services.external.rest.v1.dto.embed.TimeTableItemDto

@Mapper(componentModel = "spring", uses = [TimeTableHourMapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
abstract class TimeTableItemMapper {
	@Mappings(
		Mapping(target = "reservingRights", ignore = true)
	)
	abstract fun map(timeTableItemDto: TimeTableItemDto): TimeTableItem

	protected abstract fun mapEntity(timeTableItem: TimeTableItem): TimeTableItemDto

	fun map(timeTableItem: TimeTableItem): TimeTableItemDto {
		require(timeTableItem.reservingRights.isEmpty()) {
			"reservingRights is not supported on v1 endpoints."
		}
		return mapEntity(timeTableItem)
	}
}
