/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.constants.PropertyTypeScope
import org.taktik.icure.entities.PropertyType
import org.taktik.icure.services.external.rest.v1.dto.PropertyTypeDto
import org.taktik.icure.services.external.rest.v1.dto.constants.PropertyTypeScopeDto

@Mapper(componentModel = "spring", uses = [], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
abstract class PropertyTypeMapper {
	@Mappings(
		Mapping(target = "attachments", ignore = true),
		Mapping(target = "revHistory", ignore = true),
		Mapping(target = "conflicts", ignore = true),
		Mapping(target = "revisionsInfo", ignore = true),
	)
	abstract fun map(propertyTypeDto: PropertyTypeDto): PropertyType
	abstract fun map(propertyType: PropertyType): PropertyTypeDto

	fun map(propertyTypeScopeDto: PropertyTypeScopeDto): PropertyTypeScope = PropertyTypeScope.valueOf(propertyTypeScopeDto.name)
	fun map(propertyTypeScope: PropertyTypeScope): PropertyTypeScopeDto = PropertyTypeScopeDto.valueOf(propertyTypeScope.name)
}
