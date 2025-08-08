/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.embed.Content
import org.taktik.icure.entities.embed.Service
import org.taktik.icure.services.external.rest.v1.dto.embed.ContentDto
import org.taktik.icure.services.external.rest.v1.dto.embed.ServiceDto
import org.taktik.icure.services.external.rest.v1.mapper.base.CodeStubMapper
import org.taktik.icure.services.external.rest.v1.mapper.base.IdentifierMapper

@Mapper(componentModel = "spring", uses = [MeasureMapper::class, MedicationMapper::class, TimeSeriesMapper::class, IdentifierMapper::class, CodeStubMapper::class, DelegationMapper::class, AnnotationMapper::class, SecurityMetadataMapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface ServiceMapper {
	fun map(serviceDto: ServiceDto): Service
	fun map(service: Service): ServiceDto
	fun map(contentDto: ContentDto): Content
	fun map(content: Content): ContentDto
}
