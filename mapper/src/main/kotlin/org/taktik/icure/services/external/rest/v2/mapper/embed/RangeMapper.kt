package org.taktik.icure.services.external.rest.v2.mapper.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.embed.Measure
import org.taktik.icure.entities.embed.Range
import org.taktik.icure.services.external.rest.v2.dto.embed.MeasureDto
import org.taktik.icure.services.external.rest.v2.dto.embed.RangeDto
import org.taktik.icure.services.external.rest.v2.mapper.base.CodeStubV2Mapper

@Mapper(componentModel = "spring", uses = [], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface RangeV2Mapper {
	fun map(rangeDto: RangeDto): Range
	fun map(range: Range): RangeDto
}
