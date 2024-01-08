/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.embed.Address
import org.taktik.icure.services.external.rest.v1.dto.embed.AddressDto

@Mapper(componentModel = "spring", uses = [TelecomMapper::class, AnnotationMapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface AddressMapper {
	fun map(addressDto: AddressDto): Address

	fun map(address: Address): AddressDto
}
