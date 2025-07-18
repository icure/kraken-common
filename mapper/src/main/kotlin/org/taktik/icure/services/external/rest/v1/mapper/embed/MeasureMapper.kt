/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.embed.Measure
import org.taktik.icure.services.external.rest.v1.dto.embed.MeasureDto
import org.taktik.icure.services.external.rest.v1.mapper.base.CodeStubMapper

@Mapper(componentModel = "spring", uses = [CodeStubMapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface MeasureMapper {

	@Mappings(
		Mapping(target = "referenceRanges", ignore = true),
		Mapping(target = "valueWithPrecision", ignore = true),
	)
	fun map(measureDto: MeasureDto): Measure

	@Mappings(
		Mapping(target = "max", expression = """kotlin(measure.max ?: measure.referenceRanges.firstOrNull()?.high)"""),
		Mapping(target = "min", expression = """kotlin(measure.min ?: measure.referenceRanges.firstOrNull()?.low)"""),
		Mapping(target = "valueWithPrecision", ignore = true),
	)
	fun map(measure: Measure): MeasureDto
}
