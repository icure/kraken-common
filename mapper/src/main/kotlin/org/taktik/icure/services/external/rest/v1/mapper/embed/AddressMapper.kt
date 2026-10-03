/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.embed.Address
import org.taktik.icure.services.external.rest.v1.dto.embed.AddressDto
import org.taktik.icure.services.external.rest.v1.mapper.base.CodeStubMapper
import org.taktik.icure.services.external.rest.v1.mapper.base.IdentifierMapper

@Mapper(componentModel = "spring", uses = [TelecomMapper::class, AnnotationMapper::class, CodeStubMapper::class, IdentifierMapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface AddressMapper {
	@Mappings(
		Mapping(target = "extensions", ignore = true),
	)
	fun map(addressDto: AddressDto): Address

	fun map(address: Address): AddressDto {
		require (address.extensions == null)  { "Address has extensions and can't be used with v1 endpoints" }
		return doMap(address)
	}
	fun doMap(address: Address): AddressDto
}
