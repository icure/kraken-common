package org.taktik.icure.services.external.rest.v2.mapper.dao

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.dao.ValueFilterParameters
import org.taktik.icure.services.external.rest.v2.dto.dao.ValueFilterParametersDto

@Mapper(
	componentModel = "spring",
	uses = [RangeQueryParametersV2Mapper::class],
	injectionStrategy = InjectionStrategy.CONSTRUCTOR,
)
interface ValueFilterParametersV2Mapper {
	fun map(valueFilterParametersDto: ValueFilterParametersDto): ValueFilterParameters
	fun map(valueFilterParameters: ValueFilterParameters): ValueFilterParametersDto
}
