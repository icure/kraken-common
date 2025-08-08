/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.EntityReference
import org.taktik.icure.services.external.rest.v1.dto.EntityReferenceDto
import org.taktik.icure.services.external.rest.v1.mapper.base.IdentifierMapper
import org.taktik.icure.services.external.rest.v1.mapper.base.PropertyTypeStubMapper

@Mapper(componentModel = "spring", uses = [IdentifierMapper::class, PropertyTypeStubMapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface EntityReferenceMapper {
	@Mappings(
		Mapping(target = "attachments", ignore = true),
		Mapping(target = "revHistory", ignore = true),
		Mapping(target = "conflicts", ignore = true),
		Mapping(target = "revisionsInfo", ignore = true),
	)
	fun map(entityReferenceDto: EntityReferenceDto): EntityReference
	fun map(entityReference: EntityReference): EntityReferenceDto
}
