package org.taktik.icure.services.external.rest.v2.mapper.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.embed.ReferenceRange
import org.taktik.icure.services.external.rest.v2.dto.embed.ReferenceRangeDto
import org.taktik.icure.services.external.rest.v2.mapper.base.CodeStubV2Mapper

@Mapper(componentModel = "spring", uses = [CodeStubV2Mapper::class, RangeV2Mapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface ReferenceRangeV2Mapper {
	fun map(referenceRangeDto: ReferenceRangeDto): ReferenceRange
	fun map(referenceRange: ReferenceRange): ReferenceRangeDto
}
