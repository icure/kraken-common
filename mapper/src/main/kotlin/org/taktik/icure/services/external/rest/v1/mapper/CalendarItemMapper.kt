/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.Agenda
import org.taktik.icure.entities.CalendarItem
import org.taktik.icure.services.external.rest.v1.dto.AgendaDto
import org.taktik.icure.services.external.rest.v1.dto.CalendarItemDto
import org.taktik.icure.services.external.rest.v1.mapper.base.CodeStubMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.AddressMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.CalendarItemTagMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.DelegationMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.FlowItemMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.SecurityMetadataMapper

@Mapper(componentModel = "spring", uses = [CalendarItemTagMapper::class, CodeStubMapper::class, DelegationMapper::class, AddressMapper::class, FlowItemMapper::class, SecurityMetadataMapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
abstract class CalendarItemMapper {
	@Mappings(
		Mapping(target = "attachments", ignore = true),
		Mapping(target = "revHistory", ignore = true),
		Mapping(target = "conflicts", ignore = true),
		Mapping(target = "revisionsInfo", ignore = true),
		Mapping(target = "resourceGroup", ignore = true),
		Mapping(target = "availabilitiesAssignmentStrategy", ignore = true),
		Mapping(target = "tentativeTimestamp", ignore = true),
	)
	abstract fun map(calendarItemDto: CalendarItemDto): CalendarItem

	fun map(calendarItem: CalendarItem): CalendarItemDto {
		require(calendarItem.resourceGroup == null) { "Can't use calendar item with `resourceGroup` on v1 apis" }
		require(calendarItem.availabilitiesAssignmentStrategy == null) { "Can't use calendar item with `availabilitiesAssignmentStrategy` on v1 apis" }
		return doMap(calendarItem)
	}

	protected abstract fun doMap(calendarItem: CalendarItem): CalendarItemDto
}
