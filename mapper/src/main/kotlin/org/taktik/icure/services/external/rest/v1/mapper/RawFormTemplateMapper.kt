/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.FormTemplate
import org.taktik.icure.services.external.rest.v1.dto.FormTemplateDto
import org.taktik.icure.services.external.rest.v1.mapper.base.CodeStubMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.DocumentGroupMapper

@Mapper(componentModel = "spring", uses = [DocumentGroupMapper::class, CodeStubMapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
abstract class RawFormTemplateMapper {
	@Mappings(
		Mapping(target = "layout", ignore = true),
		Mapping(target = "templateLayout", ignore = true),
		Mapping(target = "rawTemplateLayout", expression = "kotlin(formTemplate.templateLayout ?: formTemplate.layout)"),
	)
	abstract fun map(formTemplate: FormTemplate): FormTemplateDto
}
