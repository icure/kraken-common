/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.embed.CareTeamMember
import org.taktik.icure.services.external.rest.v1.dto.embed.CareTeamMemberDto
import org.taktik.icure.services.external.rest.v1.mapper.base.CodeStubMapper

@Mapper(componentModel = "spring", uses = [CodeStubMapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface CareTeamMemberMapper {
	@Mappings(
		Mapping(target = "extensions", ignore = true),
	)
	fun map(careTeamMemberDto: CareTeamMemberDto): CareTeamMember
	fun map(careTeamMember: CareTeamMember): CareTeamMemberDto {
		require(careTeamMember.extensions == null) { "CareTeamMember has extensions and can't be used with v1 endpoints" }
		return doMap(careTeamMember)
	}
	fun doMap(careTeamMember: CareTeamMember): CareTeamMemberDto
}
