/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.Tarification
import org.taktik.icure.services.external.rest.v1.dto.TarificationDto
import org.taktik.icure.services.external.rest.v1.mapper.embed.LetterValueMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.PeriodicityMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.ValorisationMapper

@Mapper(componentModel = "spring", uses = [LetterValueMapper::class, PeriodicityMapper::class, ValorisationMapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface TarificationMapper {
	@Mappings(
		Mapping(target = "attachments", ignore = true),
		Mapping(target = "revHistory", ignore = true),
		Mapping(target = "conflicts", ignore = true),
		Mapping(target = "revisionsInfo", ignore = true),
	)
	fun map(tarificationDto: TarificationDto): Tarification
	fun map(tarification: Tarification): TarificationDto
}
