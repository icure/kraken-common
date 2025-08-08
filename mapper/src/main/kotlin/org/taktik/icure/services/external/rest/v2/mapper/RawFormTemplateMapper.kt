/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.FormTemplate
import org.taktik.icure.services.external.rest.v2.dto.FormTemplateDto
import org.taktik.icure.services.external.rest.v2.mapper.base.CodeStubV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.embed.DocumentGroupV2Mapper

@Mapper(componentModel = "spring", uses = [DocumentGroupV2Mapper::class, CodeStubV2Mapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
abstract class RawFormTemplateV2Mapper {
	@Mappings(
		Mapping(target = "layout", ignore = true),
		Mapping(target = "templateLayout", ignore = true),
		Mapping(target = "rawTemplateLayout", expression = "kotlin(formTemplate.templateLayout ?: formTemplate.layout)"),
	)
	abstract fun map(formTemplate: FormTemplate): FormTemplateDto
}
