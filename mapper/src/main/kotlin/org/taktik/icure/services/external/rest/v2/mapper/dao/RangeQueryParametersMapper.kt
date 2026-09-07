package org.taktik.icure.services.external.rest.v2.mapper.dao

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.dao.RangeQueryParameters
import org.taktik.icure.services.external.rest.v2.dto.dao.RangeQueryParametersDto

@Mapper(
	componentModel = "spring",
	uses = [KeyComponentV2Mapper::class],
	injectionStrategy = InjectionStrategy.CONSTRUCTOR,
)
interface RangeQueryParametersV2Mapper {
	fun map(rangeQueryParametersDto: RangeQueryParametersDto): RangeQueryParameters
	fun map(rangeQueryParameters: RangeQueryParameters): RangeQueryParametersDto
}
