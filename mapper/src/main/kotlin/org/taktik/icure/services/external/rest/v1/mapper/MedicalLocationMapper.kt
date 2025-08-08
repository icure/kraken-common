/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.MedicalLocation
import org.taktik.icure.services.external.rest.v1.dto.MedicalLocationDto
import org.taktik.icure.services.external.rest.v1.mapper.embed.AddressMapper

@Mapper(componentModel = "spring", uses = [AddressMapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface MedicalLocationMapper {
	@Mappings(
		Mapping(target = "attachments", ignore = true),
		Mapping(target = "revHistory", ignore = true),
		Mapping(target = "conflicts", ignore = true),
		Mapping(target = "revisionsInfo", ignore = true),
	)
	fun map(medicalLocationDto: MedicalLocationDto): MedicalLocation
	fun map(medicalLocation: MedicalLocation): MedicalLocationDto
}
