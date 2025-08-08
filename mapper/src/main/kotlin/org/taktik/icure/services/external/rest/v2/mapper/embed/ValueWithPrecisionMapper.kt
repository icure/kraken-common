package org.taktik.icure.services.external.rest.v2.mapper.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.embed.ValueWithPrecision
import org.taktik.icure.services.external.rest.v2.dto.embed.ValueWithPrecisionDto

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface ValueWithPrecisionV2Mapper {
	fun map(valueWithPrecision: ValueWithPrecision): ValueWithPrecisionDto
	fun map(valueWithPrecisionDto: ValueWithPrecisionDto): ValueWithPrecision
}
