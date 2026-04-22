/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.embed.Insurability
import org.taktik.icure.services.external.rest.v1.dto.embed.InsurabilityDto

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface InsurabilityMapper {
	@Mappings(
		Mapping(target = "extensions", ignore = true),
	)
	fun map(insurabilityDto: InsurabilityDto): Insurability
	fun map(insurability: Insurability): InsurabilityDto {
		require(insurability.extensions == null) { "Insurability has extensions and can't be used with v1 endpoints" }
		return doMap(insurability)
	}
	fun doMap(insurability: Insurability): InsurabilityDto
}
