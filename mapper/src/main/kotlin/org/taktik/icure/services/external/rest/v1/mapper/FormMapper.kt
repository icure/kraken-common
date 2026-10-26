/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.Form
import org.taktik.icure.services.external.rest.v1.dto.FormDto
import org.taktik.icure.services.external.rest.v1.mapper.base.CodeStubMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.DelegationMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.SecurityMetadataMapper

@Mapper(componentModel = "spring", uses = [CodeStubMapper::class, DelegationMapper::class, SecurityMetadataMapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface FormMapper {
	@Mappings(
		Mapping(target = "attachments", ignore = true),
		Mapping(target = "conflicts", ignore = true),
		Mapping(target = "revisionsInfo", ignore = true),
		Mapping(target = "extensions", ignore = true),
		Mapping(target = "customisedModelVersion", ignore = true),
	)
	fun map(formDto: FormDto): Form

	fun map(form: Form): FormDto {
		require(form.extensions == null) { "Form has extensions and can't be used with v1 endpoints" }
		return doMap(form)
	}

	fun doMap(form: Form): FormDto
}
