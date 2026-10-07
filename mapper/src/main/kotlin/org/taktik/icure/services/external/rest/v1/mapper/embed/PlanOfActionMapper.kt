/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.embed.PlanOfAction
import org.taktik.icure.services.external.rest.v1.dto.embed.PlanOfActionDto
import org.taktik.icure.services.external.rest.v1.mapper.base.CodeStubMapper

@Mapper(componentModel = "spring", uses = [CareTeamMembershipMapper::class, CodeStubMapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface PlanOfActionMapper {
	@Mappings(
		Mapping(target = "extensions", ignore = true),
	)
	fun map(planOfActionDto: PlanOfActionDto): PlanOfAction
	fun map(planOfAction: PlanOfAction): PlanOfActionDto {
		require(planOfAction.extensions == null) { "PlanOfAction has extensions and can't be used with v1 endpoints" }
		return doMap(planOfAction)
	}
	fun doMap(planOfAction: PlanOfAction): PlanOfActionDto
}
