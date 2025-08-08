/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.FrontEndMigration
import org.taktik.icure.services.external.rest.v1.dto.FrontEndMigrationDto
import org.taktik.icure.services.external.rest.v1.mapper.base.PropertyStubMapper

@Mapper(componentModel = "spring", uses = [PropertyStubMapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface FrontEndMigrationMapper {
	@Mappings(
		Mapping(target = "attachments", ignore = true),
		Mapping(target = "revHistory", ignore = true),
		Mapping(target = "conflicts", ignore = true),
		Mapping(target = "revisionsInfo", ignore = true),
	)
	fun map(frontEndMigrationDto: FrontEndMigrationDto): FrontEndMigration
	fun map(frontEndMigration: FrontEndMigration): FrontEndMigrationDto
}
