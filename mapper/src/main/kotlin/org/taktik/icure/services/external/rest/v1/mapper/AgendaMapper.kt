/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.Agenda
import org.taktik.icure.services.external.rest.v1.dto.AgendaDto
import org.taktik.icure.services.external.rest.v1.mapper.base.CodeStubMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.RightMapper

@Mapper(componentModel = "spring", uses = [CodeStubMapper::class, RightMapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
abstract class AgendaMapper {
	@Mappings(
		Mapping(target = "attachments", ignore = true),
		Mapping(target = "revHistory", ignore = true),
		Mapping(target = "conflicts", ignore = true),
		Mapping(target = "revisionsInfo", ignore = true),
		Mapping(target = "userRights", ignore = true),
		Mapping(target = "timeTables", ignore = true)
	)
	abstract fun map(agendaDto: AgendaDto): Agenda

	protected abstract fun mapEntity(agenda: Agenda): AgendaDto

	fun map(agenda: Agenda): AgendaDto {
		require(agenda.userRights.isEmpty()) {
			"userRights is not supported on v1 endpoints."
		}
		require(agenda.timeTables.isEmpty()) {
			"embedded timetables are not supported on v1 endpoints."
		}
		return mapEntity(agenda)
	}
}
