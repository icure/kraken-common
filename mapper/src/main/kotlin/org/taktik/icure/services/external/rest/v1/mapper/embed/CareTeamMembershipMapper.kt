/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.embed.CareTeamMembership
import org.taktik.icure.services.external.rest.v1.dto.embed.CareTeamMembershipDto

@Mapper(componentModel = "spring", uses = [], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface CareTeamMembershipMapper {
	@Mappings(
		Mapping(target = "extensions", ignore = true),
	)
	fun map(careTeamMembershipDto: CareTeamMembershipDto): CareTeamMembership
	fun map(careTeamMembership: CareTeamMembership): CareTeamMembershipDto {
		require(careTeamMembership.extensions == null) { "CareTeamMembership has extensions and can't be used with v1 endpoints" }
		return doMap(careTeamMembership)
	}
	fun doMap(careTeamMembership: CareTeamMembership): CareTeamMembershipDto
}
