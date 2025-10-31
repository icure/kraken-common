/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.embed.Medication
import org.taktik.icure.services.external.rest.v1.dto.embed.MedicationDto
import org.taktik.icure.services.external.rest.v1.mapper.base.CodeStubMapper

@Mapper(componentModel = "spring", uses = [RenewalMapper::class, MedicinalproductMapper::class, CodeStubMapper::class, RegimenItemMapper::class, SuspensionMapper::class, ParagraphAgreementMapper::class, SubstanceproductMapper::class, DurationMapper::class, AddressMapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface MedicationMapper {
	@Mappings(
		Mapping(target = "options", ignore = true),
	)
	fun map(medicationDto: MedicationDto): Medication
	fun map(medication: Medication): MedicationDto
}
