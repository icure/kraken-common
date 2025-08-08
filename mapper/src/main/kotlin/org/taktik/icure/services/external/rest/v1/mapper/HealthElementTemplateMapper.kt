/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.HealthElementTemplate
import org.taktik.icure.services.external.rest.v1.dto.HealthElementTemplateDto
import org.taktik.icure.services.external.rest.v1.mapper.base.CodeStubMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.PlanOfActionTemplateMapper

@Mapper(componentModel = "spring", uses = [PlanOfActionTemplateMapper::class, CodeStubMapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface HealthElementTemplateMapper {
	@Mappings(
		Mapping(target = "attachments", ignore = true),
		Mapping(target = "revHistory", ignore = true),
		Mapping(target = "conflicts", ignore = true),
		Mapping(target = "revisionsInfo", ignore = true),
	)
	fun map(healthElementTemplateDto: HealthElementTemplateDto): HealthElementTemplate
	fun map(healthElementTemplate: HealthElementTemplate): HealthElementTemplateDto
}
