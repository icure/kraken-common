/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.embed.SubContact
import org.taktik.icure.services.external.rest.v1.dto.embed.SubContactDto
import org.taktik.icure.services.external.rest.v1.mapper.base.CodeStubMapper

@Mapper(componentModel = "spring", uses = [CodeStubMapper::class, ServiceLinkMapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface SubContactMapper {
	@Mappings(
		Mapping(target = "extensions", ignore = true),
	)
	fun map(subContactDto: SubContactDto): SubContact
	fun map(subContact: SubContact): SubContactDto {
		require(subContact.extensions == null) { "SubContact has extensions and can't be used with v1 endpoints" }
		return doMap(subContact)
	}
	fun doMap(subContact: SubContact): SubContactDto
}
