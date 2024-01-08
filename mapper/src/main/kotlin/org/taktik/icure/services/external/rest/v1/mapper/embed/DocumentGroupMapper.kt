/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.embed.DocumentGroup
import org.taktik.icure.services.external.rest.v1.dto.embed.DocumentGroupDto

@Mapper(componentModel = "spring", uses = [], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface DocumentGroupMapper {
	fun map(documentGroupDto: DocumentGroupDto): DocumentGroup
	fun map(documentGroup: DocumentGroup): DocumentGroupDto
}
