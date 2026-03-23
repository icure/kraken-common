/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.embed.Telecom
import org.taktik.icure.services.external.rest.v1.dto.embed.TelecomDto

@Mapper(componentModel = "spring", uses = [], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface TelecomMapper {
	@Mappings(
		Mapping(target = "extensions", ignore = true),
	)
	fun map(telecomDto: TelecomDto): Telecom
	fun map(telecom: Telecom): TelecomDto {
		require (telecom.extensions == null)  { "Telecom has extensions and can't be used with v1 endpoints" }
		return doMap(telecom)
	}
	fun doMap(telecom: Telecom): TelecomDto
}
