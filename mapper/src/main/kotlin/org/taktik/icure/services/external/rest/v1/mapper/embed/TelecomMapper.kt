/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.embed.Telecom
import org.taktik.icure.services.external.rest.v1.dto.embed.TelecomDto

@Mapper(componentModel = "spring", uses = [], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface TelecomMapper {
	fun map(telecomDto: TelecomDto): Telecom
	fun map(telecom: Telecom): TelecomDto
}
