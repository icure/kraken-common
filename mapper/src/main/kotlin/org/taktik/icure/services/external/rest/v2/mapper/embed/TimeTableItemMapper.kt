/*
 *  iCure Data Stack. Copyright (c) 2020 Taktik SA
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful, but
 *     WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public
 *     License along with this program.  If not, see
 *     <https://www.gnu.org/licenses/>.
 */

package org.taktik.icure.services.external.rest.v2.mapper.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.embed.TimeTableItem
import org.taktik.icure.services.external.rest.v2.dto.embed.TimeTableItemDto

@Mapper(componentModel = "spring", uses = [TimeTableHourV2Mapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface TimeTableItemV2Mapper {
	@Mappings(
		Mapping(target = "days", ignore = true),
		Mapping(target = "recurrenceTypes", ignore = true),
	)
	fun map(timeTableItemDto: TimeTableItemDto): TimeTableItem

	fun map(timeTableItem: TimeTableItem): TimeTableItemDto
}
